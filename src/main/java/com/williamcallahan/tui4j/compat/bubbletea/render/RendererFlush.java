package com.williamcallahan.tui4j.compat.bubbletea.render;

import com.williamcallahan.tui4j.ansi.Truncate;
import com.williamcallahan.tui4j.compat.x.ansi.StringWidth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

/**
 * Diff-based rendering pipeline for the standard renderer.
 * <p>
 * Owns the render lock, output buffer, and line-diffing state. All terminal
 * writes that require synchronization go through this class.
 * <p>
 * Upstream: bubbletea/standard_renderer.go (flush/write/repaint logic)
 */
class RendererFlush {

    private static final String DISABLE_AUTO_WRAP = "\033[?7l";
    private static final String ENABLE_AUTO_WRAP = "\033[?7h";

    private final Terminal terminal;
    private final Lock renderLock = new ReentrantLock();
    private final StringBuilder buffer = new StringBuilder();
    private volatile String lastRender = "";
    private String[] lastRenderedLines = new String[0];
    private final List<String> queuedMessageLines = new ArrayList<>();
    private volatile boolean needsRender = true;
    private int linesRendered = 0;
    private int altLinesRendered = 0;
    private int lastFlushWidth = -1;
    private int lastFlushHeight = -1;
    private volatile boolean isInAltScreen;

    /**
     * Creates a flush pipeline for the given terminal.
     *
     * @param terminal JLine terminal to write output to
     */
    RendererFlush(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Diffs the current buffer against the last render and writes only changed lines.
     * <p>
     * A cached frame is only valid at the dimensions it was painted with, so a
     * changed width or height invalidates the cache here rather than relying on
     * the caller to repaint. Upstream drives this from a repaint message, but the
     * ticker thread reads its dimensions outside the render lock, so a resize can
     * slip between a repaint and the flush it was meant to force; without this
     * check that frame is painted at the old width and then diff-skipped forever.
     * <p>
     * Upstream: bubbletea/standard_renderer.go flush
     *
     * @param width  terminal width for truncation (0 = unlimited)
     * @param height terminal height for overflow trimming (0 = unlimited)
     */
    void flush(int width, int height) {
        renderLock.lock();
        try {
            if (width != lastFlushWidth || height != lastFlushHeight) {
                lastFlushWidth = width;
                lastFlushHeight = height;
                resetRenderState();
            }

            if (!needsRender) {
                return;
            }

            String frame = buffer.toString();
            if (queuedMessageLines.isEmpty()
                && (frame.isEmpty() || frame.equals(lastRender))) {
                return;
            }

            StringBuilder out = new StringBuilder();
            String[] newLines = splitAndTruncateHeight(frame, height);
            boolean repainted = lastRenderedLines.length == 0;

            if (isInAltScreen) {
                out.append(DISABLE_AUTO_WRAP);
            } else if (linesRendered > 1) {
                out.append("\033[").append(linesRendered - 1).append("A");
            }

            int queuedLines = flushQueuedMessages(out, width);
            renderDiffLines(out, newLines, queuedLines > 0, width);

            if (needsEraseBelow(repainted, queuedLines + newLines.length)
                && (height <= 0 || newLines.length < height)) {
                if (isInAltScreen) {
                    appendCursorPosition(out, newLines.length + 1);
                    out.append("\033[J");
                } else {
                    // Step onto the row below the content and return to column 1
                    // before erasing: erasing from the last row itself wipes a cached
                    // final row that was skipped unchanged (and the last cell of a
                    // full-width row). CUD clamps at the bottom margin instead of
                    // scrolling; CUU restores the origin the next flush assumes.
                    out.append("\033[B\r\033[J\033[A");
                }
            }

            if (isInAltScreen) {
                altLinesRendered = newLines.length;
                out.append(ENABLE_AUTO_WRAP);
                appendCursorPosition(out, newLines.length);
            } else {
                linesRendered = newLines.length;
                out.append("\r");
            }

            terminal.writer().print(out);
            terminal.writer().flush();

            lastRender = frame;
            lastRenderedLines = newLines;
            needsRender = false;
        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Splits the buffer into lines and trims overflow beyond the given height.
     * <p>
     * Splits with a negative limit because Java drops trailing empty strings,
     * while upstream {@code strings.Split} keeps them: a view ending in a
     * newline owns a real final blank row, and dropping it makes the cached line
     * count and the physical rows disagree.
     * <p>
     * Upstream: bubbletea/standard_renderer.go flush (strings.Split)
     *
     * @param frame the view being flushed
     * @param height terminal rows, or nonpositive for no limit
     * @return the rows to paint
     */
    private String[] splitAndTruncateHeight(String frame, int height) {
        String[] newLines = frame.split("\n", -1);
        if (height > 0 && newLines.length > height) {
            newLines = Arrays.copyOfRange(newLines, newLines.length - height, newLines.length);
        }
        return newLines;
    }

    /** Appends queued print-line messages and returns the physical rows they occupy; skips alt-screen mode. */
    private int flushQueuedMessages(StringBuilder out, int width) {
        if (queuedMessageLines.isEmpty() || isInAltScreen) {
            return 0;
        }
        int queuedRows = 0;
        for (String line : queuedMessageLines) {
            int lineWidth = StringWidth.stringWidth(line);
            if (width <= 0) {
                out.append("\033[K").append(line);
            } else if (lineWidth < width) {
                out.append(line).append("\033[K");
            } else {
                out.append(line);
            }
            out.append("\r\n");
            // A line wider than the terminal wraps natively onto extra physical rows.
            queuedRows += width > 0 ? Math.max(1, (lineWidth + width - 1) / width) : 1;
        }
        queuedMessageLines.clear();
        return queuedRows;
    }

    /**
     * Emits changed lines without repainting unchanged physical rows.
     * <p>
     * Alternate-screen rows use absolute positions rather than line feeds. A
     * terminal may assign a different width to a joined Unicode sequence than
     * the grapheme contract used to build the view. Absolute positioning plus
     * disabled autowrap keeps that disagreement inside its owning row.
     * Current upstream uses absolute coordinates for fullscreen rendering in
     * {@code bubbletea/cursed_renderer.go}.
     * <p>
     * With a known width, every row is written before its remainder is erased, as upstream
     * {@code bubbletea/standard_renderer.go} does: terminals read output in chunks and may
     * paint between reads, so clearing a row first blanks it whenever a read ends inside
     * the frame. A row measured to fill the terminal gets no erase, because erasing from
     * the last column would wipe its final cell; as upstream, a terminal that renders such a
     * row narrower keeps the previous frame's final cell. With an unknown width, erase
     * before writing: delayed autowrap may leave the cursor on an occupied final cell,
     * so an erase after writing could delete the row's last character.
     */
    private void renderDiffLines(StringBuilder out, String[] newLines, boolean forceRender, int width) {
        for (int i = 0; i < newLines.length; i++) {
            boolean canSkip =
                !forceRender &&
                lastRenderedLines.length > i &&
                newLines[i].equals(lastRenderedLines[i]);

            if (canSkip) {
                if (!isInAltScreen && i < newLines.length - 1) {
                    out.append("\n");
                }
                continue;
            }

            String line = newLines[i];

            if (width > 0) {
                line = Truncate.truncate(line, width, "");
            }

            if (isInAltScreen) {
                appendCursorPosition(out, i + 1);
            } else {
                out.append("\r");
            }
            if (width <= 0) {
                out.append("\033[K");
            }
            out.append(line);
            if (width > 0 && StringWidth.stringWidth(line) < width) {
                out.append("\033[K");
            }

            if (!isInAltScreen && i < newLines.length - 1) {
                out.append("\r\n");
            }
        }
    }

    /** Appends a one-based alternate-screen cursor position at column one. */
    private static void appendCursorPosition(StringBuilder out, int row) {
        out.append("\033[").append(row).append(";1H");
    }

    /**
     * Replaces the render buffer with the latest view string.
     * <p>
     * Upstream: bubbletea/standard_renderer.go write
     *
     * @param view      rendered model output
     * @param isRunning ignored when false (program shutting down)
     */
    void write(String view, boolean isRunning) {
        if (!isRunning) return;

        String string = view.isEmpty() ? " " : view;

        renderLock.lock();
        try {
            buffer.setLength(0);
            buffer.append(string);
            needsRender = true;
        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Resets cached render state so the next flush redraws everything.
     * <p>
     * Acquires the render lock because external callers (e.g. StandardRenderer)
     * may invoke this without already holding it. Internal callers that already
     * hold the lock must use {@link #resetRenderState()} instead.
     * <p>
     * Upstream: bubbletea/standard_renderer.go repaint
     */
    void repaint() {
        renderLock.lock();
        try {
            resetRenderState();
        } finally {
            renderLock.unlock();
        }
    }

    /** Clears cached render state. Caller must already hold {@code renderLock}. */
    private void resetRenderState() {
        lastRender = "";
        lastRenderedLines = new String[0];
        needsRender = true;
    }

    /** Returns the line count for the active terminal screen. */
    private int lastLinesRendered() {
        return isInAltScreen ? altLinesRendered : linesRendered;
    }

    /**
     * Reports whether rows below the painted content must be erased this flush.
     * <p>
     * A repaint in the alternate screen must clear everything under the view,
     * because the terminal may have reflowed previous content into those rows on
     * resize; the renderer owns the whole alternate screen. The normal screen
     * only erases leftovers below a previously taller region, counting queued
     * output above the new view so no final view row is erased at the bottom
     * margin. Unlike upstream bubbletea/standard_renderer.go v1.3.10, queued
     * rows participate in the erase comparison because they occupy old rows.
     *
     * @param repainted whether the render cache was empty (repaint, clear, resize, or screen transition)
     * @param visibleLines rows occupied by queued output and the view this flush
     * @return {@code true} when the area below the content should be cleared
     */
    private boolean needsEraseBelow(boolean repainted, int visibleLines) {
        if (isInAltScreen) {
            return repainted || lastLinesRendered() > visibleLines;
        }
        return lastLinesRendered() > visibleLines;
    }

    /** Marks the renderer as needing a redraw on the next tick (tui4j extension). */
    void notifyModelChanged() {
        this.needsRender = true;
    }

    /**
     * Queues a message for printing above the rendered view on the next flush.
     * Ignored in alt-screen mode because queued messages use inline scrollback.
     *
     * @param messageBody text to print (may contain newlines)
     */
    void queuePrintLine(String messageBody) {
        renderLock.lock();
        try {
            if (isInAltScreen) {
                return;
            }
            String[] lines = messageBody.split("\n", -1);
            queuedMessageLines.addAll(Arrays.asList(lines));
            needsRender = true;
            resetRenderState();
        } finally {
            renderLock.unlock();
        }
    }

    /** Shows the terminal cursor. Upstream: bubbletea/standard_renderer.go showCursor. */
    void showCursor() {
        renderLock.lock();
        try {
            terminal.puts(InfoCmp.Capability.cursor_visible);
            terminal.flush();
        } finally {
            renderLock.unlock();
        }
    }

    /** Hides the terminal cursor. Upstream: bubbletea/standard_renderer.go hideCursor. */
    void hideCursor() {
        renderLock.lock();
        try {
            terminal.puts(InfoCmp.Capability.cursor_invisible);
            terminal.flush();
        } finally {
            renderLock.unlock();
        }
    }

    /** Clears the screen and resets render state. Upstream: bubbletea/standard_renderer.go clearScreen. */
    void clearScreen() {
        renderLock.lock();
        try {
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();
            resetRenderState();
        } finally {
            renderLock.unlock();
        }
    }

    /** Switches to the alternate screen buffer. Upstream: bubbletea/standard_renderer.go enterAltScreen. */
    void enterAltScreen() {
        renderLock.lock();
        try {
            if (isInAltScreen) return;
            if (terminal.getType().equals("dumb")) return;

            terminal.puts(InfoCmp.Capability.enter_ca_mode);
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.puts(InfoCmp.Capability.cursor_home);

            resetRenderState();
            isInAltScreen = true;
            altLinesRendered = 0;

            terminal.flush();
        } finally {
            renderLock.unlock();
        }
    }

    /** Returns from the alternate screen buffer. Upstream: bubbletea/standard_renderer.go exitAltScreen. */
    void exitAltScreen() {
        renderLock.lock();
        try {
            if (!isInAltScreen) return;
            terminal.puts(InfoCmp.Capability.exit_ca_mode);

            resetRenderState();
            isInAltScreen = false;

            terminal.flush();
        } finally {
            renderLock.unlock();
        }
    }

    /** Returns whether the terminal is currently in alternate-screen mode. */
    boolean altScreen() {
        return isInAltScreen;
    }

    /**
     * Moves the cursor below the last normal-screen frame so the shell prompt starts under it.
     * <p>
     * Upstream standard_renderer.go stop erases the final row instead; tui4j keeps it because
     * full-frame views such as Brief's bordered layout otherwise lose their bottom edge. CRLF
     * scrolls at the bottom margin, where a cursor-down capability may not. The alternate
     * screen restores the launch cursor on exit, so it needs no advance.
     */
    void moveBelowFrame() {
        renderLock.lock();
        try {
            if (isInAltScreen || linesRendered == 0) {
                return;
            }
            terminal.writer().print("\r\n");
            terminal.writer().flush();
        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Writes a value to the terminal under the render lock.
     *
     * @param value escape code or content to write
     */
    void writeToTerminal(String value) {
        renderLock.lock();
        try {
            terminal.writer().print(value);
            terminal.writer().flush();
        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Sets the terminal window title via OSC 2 escape sequence.
     * <p>
     * Delegates to {@link #writeToTerminal(String)} to serialize the write
     * with other flush output.
     * <p>
     * Upstream: bubbletea/standard_renderer.go setWindowTitle
     *
     * @param title window title text
     */
    void setWindowTitle(String title) {
        writeToTerminal("\u001b]2;" + title + "\u0007");
    }
}
