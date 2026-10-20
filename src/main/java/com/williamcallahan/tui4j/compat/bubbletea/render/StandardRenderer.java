package com.williamcallahan.tui4j.compat.bubbletea.render;

import com.williamcallahan.tui4j.ansi.Code;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.WindowSizeMessage;
import com.williamcallahan.tui4j.term.Clipboard;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Terminal;

/**
 * Default renderer backed by JLine.
 * tui4j:
 * src/main/java/com/williamcallahan/tui4j/compat/bubbletea/render/StandardRenderer.java
 */
public class StandardRenderer implements Renderer {

    private static final int DEFAULT_FPS = 60;

    /**
     * Erases the entire line and returns the cursor to column one.
     * <p>
     * Upstream: bubbletea/standard_renderer.go stop and kill end with
     * {@code EraseEntireLine} + {@code "\r"} so the shell prompt replaces the final row.
     */
    private static final String ERASE_LINE = "\u001b[2K\r";

    private volatile boolean isRunning = false;
    private final ScheduledExecutorService ticker;
    private final long frameTime;
    private int width;
    private int height;
    private volatile boolean isReportFocus;
    private boolean bracketedPasteEnabled;
    private boolean kittyKeyboardEnabled;
    private final RendererFlush rendererFlush;

    /**
     * Creates a renderer with the default frame rate.
     *
     * @param terminal JLine terminal
     */
    public StandardRenderer(Terminal terminal) {
        this(terminal, DEFAULT_FPS);
    }

    /**
     * Creates a renderer with a custom frame rate.
     *
     * @param terminal JLine terminal
     * @param fps frames per second (clamped to 1-120)
     */
    public StandardRenderer(Terminal terminal, int fps) {
        this.frameTime = 1000 / Math.clamp(fps, 1, 120);
        this.rendererFlush = new RendererFlush(terminal);
        this.ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tui4j-Renderer-Thread");
            t.setDaemon(true);
            return t;
        });

        try {
            this.width = terminal.getWidth();
            this.height = terminal.getHeight();
        } catch (Exception ignored) {
            // Fallback to 80x24: acceptable because handleMessage(WindowSizeMessage)
            // updates width/height on every terminal resize event, so the renderer
            // self-corrects as soon as the first resize signal arrives.
            this.width = 80;
            this.height = 24;
        }
    }

    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go start behavior.
    public void start() {
        if (!isRunning) {
            isRunning = true;
            ticker.scheduleAtFixedRate(
                this::flush,
                0,
                frameTime,
                TimeUnit.MILLISECONDS
            );
        }
    }

    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go stop behavior.
    public void stop() {
        flush();
        rendererFlush.writeToTerminal(ERASE_LINE);
        isRunning = false;
        try {
            ticker.shutdownNow();
            ticker.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void pause() {
        isRunning = false;
    }

    @Override
    public void resume() {
        isRunning = true;
    }

    private void flush() {
        rendererFlush.flush(width, height);
    }

    @Override
    public void write(String view) {
        rendererFlush.write(view, isRunning);
    }

    @Override
    public void showCursor() {
        rendererFlush.showCursor();
    }

    @Override
    public void hideCursor() {
        rendererFlush.hideCursor();
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // enableMouseCellMotion behavior.
    public void enableMouseCellMotion() {
        rendererFlush.writeToTerminal(Code.EnableMouseCellMotion.value());
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // disableMouseCellMotion behavior.
    public void disableMouseCellMotion() {
        rendererFlush.writeToTerminal(Code.DisableMouseCellMotion.value());
    }

    @Override
    public void enableMouseNormalTracking() {
        rendererFlush.writeToTerminal(Code.EnableMouseNormalTracking.value());
    }

    @Override
    public void disableMouseNormalTracking() {
        rendererFlush.writeToTerminal(Code.DisableMouseNormalTracking.value());
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // enableMouseAllMotion behavior.
    public void enableMouseAllMotion() {
        rendererFlush.writeToTerminal(Code.EnableMouseAllMotion.value());
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // disableMouseAllMotion behavior.
    public void disableMouseAllMotion() {
        rendererFlush.writeToTerminal(Code.DisableMouseAllMotion.value());
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // enableMouseSGRMode behavior.
    public void enableMouseSGRMode() {
        rendererFlush.writeToTerminal(Code.EnableMouseSgrExt.value());
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // disableMouseSGRMode behavior.
    public void disableMouseSGRMode() {
        rendererFlush.writeToTerminal(Code.DisableMouseSgrExt.value());
    }

    @Override
    // tui4j extension; no Bubble Tea equivalent.
    public void setMouseCursorText() {
        rendererFlush.writeToTerminal(Code.SetMouseTextCursor.value());
    }

    @Override
    // tui4j extension; no Bubble Tea equivalent.
    public void setMouseCursorPointer() {
        rendererFlush.writeToTerminal(Code.SetMousePointerCursor.value());
    }

    @Override
    // tui4j extension; no Bubble Tea equivalent.
    public void resetMouseCursor() {
        rendererFlush.writeToTerminal(Code.ResetMouseCursor.value());
    }

    /**
     * Copies text to the clipboard via local mechanisms and OSC 52.
     * <p>
     * tui4j extension with no Bubble Tea equivalent. Attempts local clipboard
     * copy via {@link Clipboard#tryCopy}, then emits OSC 52 for remote/SSH terminals.
     * Local copy is best-effort; OSC 52 always emitted for terminal support.
     *
     * @param text the text to copy to clipboard
     */
    @Override
    public void copyToClipboard(String text) {
        Clipboard.tryCopy(text);
        rendererFlush.writeToTerminal(Code.copyToClipboard(text));
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go clearScreen
    // behavior.
    public void clearScreen() {
        rendererFlush.clearScreen();
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go altScreen
    // behavior.
    public boolean altScreen() {
        return rendererFlush.altScreen();
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go enterAltScreen
    // behavior.
    public void enterAltScreen() {
        rendererFlush.enterAltScreen();
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go exitAltScreen
    // behavior.
    public void exitAltScreen() {
        rendererFlush.exitAltScreen();
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go reportFocus
    // behavior.
    public boolean reportFocus() {
        return isReportFocus;
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // enableReportFocus behavior.
    public void enableReportFocus() {
        isReportFocus = true;
        rendererFlush.writeToTerminal(Code.EnableFocusReporting.value());
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go
    // disableReportFocus behavior.
    public void disableReportFocus() {
        isReportFocus = false;
        rendererFlush.writeToTerminal(Code.DisableFocusReporting.value());
    }

    @Override
    public void enableBracketedPaste() {
        rendererFlush.writeToTerminal(Code.EnableBracketedPaste.value());
        bracketedPasteEnabled = true;
    }

    @Override
    public void disableBracketedPaste() {
        rendererFlush.writeToTerminal(Code.DisableBracketedPaste.value());
        bracketedPasteEnabled = false;
    }

    @Override
    public boolean bracketedPaste() {
        return bracketedPasteEnabled;
    }

    @Override
    public void enableKittyKeyboard() {
        rendererFlush.writeToTerminal(Code.EnableKittyKeyboard.value());
        kittyKeyboardEnabled = true;
    }

    @Override
    public void disableKittyKeyboard() {
        rendererFlush.writeToTerminal(Code.DisableKittyKeyboard.value());
        kittyKeyboardEnabled = false;
    }

    @Override
    public boolean kittyKeyboard() {
        return kittyKeyboardEnabled;
    }

    @Override
    // tui4j extension; no Bubble Tea equivalent.
    public void notifyModelChanged() {
        rendererFlush.notifyModelChanged();
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go repaint
    // behavior.
    public void repaint() {
        rendererFlush.repaint();
    }

    @Override
    // Bubble Tea: seeks to replicate bubbletea/standard_renderer.go handleMessages
    // behavior.
    public void handleMessage(Message msg) {
        WindowSizeMessage wsm = RendererMessageDispatcher.dispatch(msg, this, rendererFlush);
        if (wsm != null) {
            this.width = wsm.width();
            this.height = wsm.height();
            rendererFlush.notifyModelChanged();
            rendererFlush.repaint();
        }
    }
}
