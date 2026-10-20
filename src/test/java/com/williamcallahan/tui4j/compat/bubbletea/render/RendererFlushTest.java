package com.williamcallahan.tui4j.compat.bubbletea.render;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies renderer output against physical terminal cells and cursor state.
 */
class RendererFlushTest {

    private static final String RED = "\u001b[31m";
    private static final String RESET = "\u001b[0m";

    /**
     * Proves that alternate-screen repaint origin is independent of resize reflow.
     */
    @Test
    void alternateScreenRepaintStartsAtHomeAfterFullWidthResizeReflow() {
        RendererTestTerminal terminal = new RendererTestTerminal(12, 8);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(12, 8);
        flush.enterAltScreen();
        terminal.drain();

        render(flush, terminal, screen, 12, 8, frame(12, "Working 1"));
        terminal.resize(10, 8);
        screen.resize(10, 8);
        flush.repaint();

        render(flush, terminal, screen, 10, 8, frame(10, "Working 2"));

        assertThat(screen.line(1)).isBlank();
        assertThat(screen.line(0)).isEqualTo("Working 2 ");
        assertThat(screen.line(2)).isEqualTo("──────────");
        assertThat(screen.cursorRow()).isEqualTo(5);
        assertThat(screen.cursorColumn()).isZero();
    }

    /**
     * Proves that ANSI bytes never prevent erasure when a visible row shrinks.
     */
    @Test
    void styledShorterReplacementErasesOldVisibleSuffix() {
        RendererTestTerminal terminal = new RendererTestTerminal(10, 4);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(10, 4);
        flush.enterAltScreen();
        terminal.drain();

        render(flush, terminal, screen, 10, 4, "123456789\nfooter");
        render(flush, terminal, screen, 10, 4, RED + "X" + RESET + "\nfooter");

        assertThat(screen.line(0)).isEqualTo("X         ");
    }

    /**
     * Sustains an exact-height alternate-screen layout across changing status frames.
     */
    @Test
    void terminalHeightFramesKeepOneStatusAndBlankComposer() {
        int width = 24;
        int height = 8;
        RendererTestTerminal terminal = new RendererTestTerminal(width, height);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, height);
        flush.enterAltScreen();
        terminal.drain();

        for (int frame = 0; frame < 1_000; frame++) {
            String status = RED + (frame % 2 == 0 ? "◐" : "◓")
                + " Working " + frame + RESET;
            String view = String.join("\n",
                "transcript one",
                "transcript two",
                pad(status, width),
                RED + " " + RESET,
                "─".repeat(width),
                "/project/path",
                "stats             model",
                "footer"
            );
            render(flush, terminal, screen, width, height, view);

            assertThat(screen.line(2)).contains("Working " + frame);
            assertThat(screen.line(3)).isBlank();
            assertThat(screen.line(4)).isEqualTo("─".repeat(width));
        }
        assertThat(screen.cursorRow()).isEqualTo(height - 1);
        assertThat(screen.cursorColumn()).isZero();
    }

    /**
     * Proves a height resize invalidates cached row placement before repainting.
     */
    @Test
    void heightResizeRepaintsAtHomeAndPlacesCursorExplicitly() {
        int width = 16;
        RendererTestTerminal terminal = new RendererTestTerminal(width, 8);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, 8);
        flush.enterAltScreen();
        terminal.drain();
        render(flush, terminal, screen, width, 8, exactHeightFrame(width, 8, "Working 1"));

        terminal.resize(width, 6);
        screen.resize(width, 6);
        flush.repaint();
        render(flush, terminal, screen, width, 6, exactHeightFrame(width, 6, "Working 2"));

        assertThat(screen.line(0)).contains("Working 2");
        assertThat(screen.line(1)).isBlank();
        assertThat(screen.line(2)).isEqualTo("─".repeat(width));
        assertThat(screen.cursorRow()).isEqualTo(5);
        assertThat(screen.cursorColumn()).isZero();
    }

    /**
     * Proves repaint redraws an unchanged logical frame after physical cells are lost.
     */
    @Test
    void repaintRestoresUnchangedFrameAfterPhysicalClear() {
        RendererTestTerminal terminal = new RendererTestTerminal(12, 6);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(12, 6);
        flush.enterAltScreen();
        terminal.drain();
        String view = frame(12, "Working");
        render(flush, terminal, screen, 12, 6, view);

        screen.clear();
        flush.repaint();
        flush.flush(12, 6);
        screen.apply(terminal.drain());

        assertThat(screen.line(0)).startsWith("Working");
        assertThat(screen.line(1)).isBlank();
        assertThat(screen.line(2)).isEqualTo("─".repeat(12));
    }

    /**
     * Proves clear marks cached content for redraw even when the model is unchanged.
     */
    @Test
    void clearScreenRestoresUnchangedFrameOnNextFlush() {
        RendererTestTerminal terminal = new RendererTestTerminal(12, 6);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(12, 6);
        flush.enterAltScreen();
        terminal.drain();
        String view = frame(12, "Working");
        render(flush, terminal, screen, 12, 6, view);

        flush.clearScreen();
        terminal.drain();
        screen.clear();
        flush.flush(12, 6);
        screen.apply(terminal.drain());

        assertThat(screen.line(0)).startsWith("Working");
        assertThat(screen.line(1)).isBlank();
        assertThat(screen.line(2)).isEqualTo("─".repeat(12));
    }

    /**
     * Proves normal-screen diffing and queued scrollback remain relative and ordered.
     */
    @Test
    void normalScreenKeepsRelativeDiffAndQueuedPrintBehavior() {
        RendererTestTerminal terminal = new RendererTestTerminal(14, 6);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(14, 6);
        screen.positionCursor(2, 0);

        render(flush, terminal, screen, 14, 6, "status 1\nfooter");
        flush.queuePrintLine("notice");
        flush.write("status 2\nfooter", true);
        flush.flush(14, 6);
        String output = terminal.drain();
        screen.apply(output);

        assertThat(output).doesNotContain("\u001b[H");
        assertThat(screen.line(2)).startsWith("notice");
        assertThat(screen.line(3)).startsWith("status 2");
        assertThat(screen.line(4)).startsWith("footer");
        assertThat(screen.cursorRow()).isEqualTo(4);
        assertThat(screen.cursorColumn()).isZero();
    }

    /**
     * Proves entering and leaving alternate screen preserves normal-screen line ownership.
     */
    @Test
    void alternateScreenUsesIndependentLineCountFromNormalScreen() {
        RendererTestTerminal terminal = new RendererTestTerminal(14, 8);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel normalScreen = new TerminalCellModel(14, 8);
        normalScreen.positionCursor(3, 0);
        render(flush, terminal, normalScreen, 14, 8, "normal 1\nnormal 2");

        flush.enterAltScreen();
        terminal.drain();
        TerminalCellModel alternateScreen = new TerminalCellModel(14, 8);
        render(flush, terminal, alternateScreen, 14, 8, exactHeightFrame(14, 8, "Working"));
        flush.exitAltScreen();
        terminal.drain();

        flush.write("normal changed\nnormal 2", true);
        flush.flush(14, 8);
        normalScreen.apply(terminal.drain());

        assertThat(normalScreen.line(3)).startsWith("normal changed");
        assertThat(normalScreen.line(4)).startsWith("normal 2");
        assertThat(normalScreen.cursorRow()).isEqualTo(4);
    }

    /**
     * Proves a view that ends in a newline owns that final blank row.
     * <p>
     * Upstream splits the view with Go's {@code strings.Split}, which keeps
     * trailing empty fields; Java's {@code split} drops them. The line count
     * feeds height trimming and the erase cache, so dropping the row shifts the
     * whole frame by one.
     */
    @Test
    void trailingNewlineKeepsFinalBlankRow() {
        int width = 10;
        int height = 2;
        RendererTestTerminal terminal = new RendererTestTerminal(width, height);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, height);
        flush.enterAltScreen();
        terminal.drain();

        render(flush, terminal, screen, width, height, "alpha\nbravo\n");

        assertThat(screen.line(0)).startsWith("bravo");
        assertThat(screen.line(1)).isBlank();
    }

    /**
     * Proves erasing rows below a shrinking frame preserves the frame's own
     * final row when it was skipped unchanged, instead of wiping it.
     */
    @Test
    void shrinkingFramePreservesSkippedFinalRow() {
        int width = 12;
        int height = 6;
        RendererTestTerminal terminal = new RendererTestTerminal(width, height);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, height);
        flush.enterAltScreen();
        terminal.drain();

        render(flush, terminal, screen, width, height, "alpha\nbravo\ncharlie\ndelta\necho");
        render(flush, terminal, screen, width, height, "alpha\nbravo\ncharlie");

        assertThat(screen.line(2)).startsWith("charlie");
        assertThat(screen.line(3)).isBlank();
    }

    /**
     * Proves a repaint after a resize clears rows the terminal reflowed below a
     * shorter view; otherwise the previous frame's rows linger beneath it.
     */
    @Test
    void repaintAfterResizeClearsRowsBelowShorterView() {
        int width = 12;
        int height = 4;
        RendererTestTerminal terminal = new RendererTestTerminal(width, height);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, height);
        flush.enterAltScreen();
        terminal.drain();
        render(flush, terminal, screen, width, height,
            "0123456789AB\nCDEFGHIJKLMN\nOPQRSTUVWXYZ\nabcdefghijkl");

        terminal.resize(6, 6);
        screen.resize(6, 6);
        flush.repaint();
        render(flush, terminal, screen, 6, 6, "aaaaaa\nbbbbbb\ncccccc\ndddddd");

        assertThat(screen.line(3)).isEqualTo("dddddd");
        assertThat(screen.line(4)).isBlank();
        assertThat(screen.line(5)).isBlank();
    }

    /**
     * Proves a changed terminal width invalidates the cached frame even when the
     * caller has not repainted, because the ticker can read its dimensions before
     * a resize is published and rebuild the cache at the old width.
     */
    @Test
    void widthChangeInvalidatesCachedFrameWithoutRepaint() {
        int width = 20;
        int height = 6;
        RendererTestTerminal terminal = new RendererTestTerminal(width, height);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, height);
        flush.enterAltScreen();
        terminal.drain();
        String view = "0123456789abcdefghij\nfooter";
        render(flush, terminal, screen, width, height, view);

        terminal.resize(10, height);
        screen.resize(10, height);
        flush.repaint();

        // A tick that captured the old width before the resize was published
        // repaints and rebuilds the cache from the unchanged buffer.
        flush.write(view, true);
        flush.flush(width, height);
        screen.apply(terminal.drain());

        // The next flush sees the new width and must repaint, not diff-skip.
        flush.flush(10, height);
        String output = terminal.drain();
        screen.apply(output);

        assertThat(output).isNotEmpty();
        assertThat(screen.line(0)).isEqualTo("0123456789");
        assertThat(screen.line(1)).isEqualTo("footer    ");
        assertThat(screen.line(2)).isBlank();
    }

    /**
     * Proves a view whose only change is a status row stays aligned when a
     * transcript row carries an emoji the terminal draws two cells wide.
     * <p>
     * The cell model measures with the canonical width contract, so a terminal
     * that draws U+2705 (East Asian Wide) in two cells is modelled by
     * substituting an equivalent two-cell code point for it before applying the
     * emitted bytes. If the width contract under-reports the emoji, the padded
     * row is one cell wider than the terminal, wraps the right margin, and the
     * wrapping frame strands the previous status in the composer row.
     */
    @Test
    void fullWidthRowWithWideEmojiKeepsComposerBlank() {
        int width = 24;
        int height = 8;
        RendererTestTerminal terminal = new RendererTestTerminal(width, height);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, height);
        flush.enterAltScreen();
        terminal.drain();

        for (int frame = 0; frame < 2; frame++) {
            String transcript = RED + "\u2705 build passed " + frame + RESET;
            renderWide(flush, terminal, screen, width, height,
                frameWithTranscript(width, transcript, "Working " + frame));
        }

        assertThat(screen.line(2)).startsWith("Working 1");
        assertThat(screen.line(3)).isBlank();
        assertThat(screen.line(4)).isEqualTo("─".repeat(width));
    }

    /**
     * Builds an exact-height frame whose first transcript row carries the given text.
     *
     * @param width terminal columns
     * @param transcript first transcript row
     * @param status changing status text
     * @return logical frame
     */
    private static String frameWithTranscript(int width, String transcript, String status) {
        return String.join("\n",
            pad(transcript, width),
            pad("second transcript", width),
            pad(status, width),
            pad(RED + " " + RESET, width),
            pad("─".repeat(width), width),
            pad("/project/path", width),
            pad("stats             model", width),
            pad("footer", width)
        );
    }

    /**
     * Writes and flushes one frame, modelling the terminal's two-cell rendering
     * of U+2705 by substituting an equivalent two-cell code point (你).
     *
     * @param flush renderer flush pipeline
     * @param terminal output-capturing terminal
     * @param screen physical terminal model
     * @param width terminal columns
     * @param height terminal rows
     * @param view logical frame
     */
    private static void renderWide(RendererFlush flush, RendererTestTerminal terminal, TerminalCellModel screen,
                                   int width, int height, String view) {
        flush.write(view, true);
        flush.flush(width, height);
        screen.apply(terminal.drain().replace("\u2705", "\u4F60"));
    }

    /**
     * Writes and flushes one logical frame into the physical terminal model.
     *
     * @param flush renderer flush pipeline
     * @param terminal output-capturing terminal
     * @param screen physical terminal model
     * @param width terminal columns
     * @param height terminal rows
     * @param view logical frame
     */
    private static void render(
        RendererFlush flush,
        RendererTestTerminal terminal,
        TerminalCellModel screen,
        int width,
        int height,
        String view
    ) {
        flush.write(view, true);
        flush.flush(width, height);
        screen.apply(terminal.drain());
    }

    /**
     * Builds the shorter-than-terminal frame used to expose resize origin drift.
     *
     * @param width terminal columns
     * @param status changing status text
     * @return logical frame
     */
    private static String frame(int width, String status) {
        return String.join("\n",
            status,
            RED + " " + RESET,
            "─".repeat(width),
            "/path",
            "stats",
            "footer"
        );
    }

    /**
     * Builds a frame whose logical row count equals the terminal height.
     *
     * @param width terminal columns
     * @param height terminal rows
     * @param status changing status text
     * @return exact-height logical frame
     */
    private static String exactHeightFrame(int width, int height, String status) {
        String[] lines = new String[height];
        lines[0] = status;
        lines[1] = RED + " " + RESET;
        lines[2] = "─".repeat(width);
        for (int index = 3; index < height; index++) lines[index] = "footer " + index;
        return String.join("\n", lines);
    }

    /**
     * Pads a styled status line to an exact display width for margin coverage.
     *
     * @param status styled status text
     * @param width target display width
     * @return padded status
     */
    private static String pad(String status, int width) {
        int visibleWidth = com.williamcallahan.tui4j.compat.x.ansi.StringWidth.stringWidth(status);
        return status + " ".repeat(width - visibleWidth);
    }
}
