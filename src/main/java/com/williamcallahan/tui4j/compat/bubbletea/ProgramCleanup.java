package com.williamcallahan.tui4j.compat.bubbletea;

import com.williamcallahan.tui4j.compat.bubbletea.input.InputHandler;
import com.williamcallahan.tui4j.compat.bubbletea.render.Renderer;
import com.williamcallahan.tui4j.runtime.CommandExecutor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jline.terminal.Terminal;

/**
 * Cleanup and terminal restoration logic for {@link Program}.
 * <p>
 * Upstream: bubbletea/tea.go
 *
 * @see Program
 * @see ProgramCore
 */
final class ProgramCleanup {

    private ProgramCleanup() {
    }

    /**
     * Restores terminal state and releases resources.
     *
     * @param renderFinalView whether the final model view should be rendered
     * @param finalModel final model (only used when {@code renderFinalView} is true)
     * @param inputHandler input handler to stop
     * @param renderer renderer to stop and restore
     * @param mouseHandler mouse handler for auto-scroll/cursor cleanup
     * @param terminal terminal to restore/close
     * @param programTerminal terminal wrapper used to close opened input
     * @param isRunning running flag to clear
     * @param commandExecutor command executor to shut down
     * @param lastError last error encountered during the run (for context chaining)
     */
    static void cleanup(
        boolean renderFinalView,
        Model finalModel,
        InputHandler inputHandler,
        Renderer renderer,
        ProgramMouseHandler mouseHandler,
        Terminal terminal,
        ProgramTerminal programTerminal,
        AtomicBoolean isRunning,
        CommandExecutor commandExecutor,
        Throwable lastError
    ) {
        Objects.requireNonNull(inputHandler, "inputHandler");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(terminal, "terminal");
        Objects.requireNonNull(isRunning, "isRunning");
        Objects.requireNonNull(commandExecutor, "commandExecutor");

        inputHandler.stop();

        if (renderFinalView) {
            Objects.requireNonNull(finalModel, "finalModel");
            renderer.write(finalModel.view());
        }
        renderer.showCursor();
        renderer.stop();

        if (mouseHandler != null) {
            mouseHandler.stopAutoScroll();
        }

        if (renderer.bracketedPaste()) {
            renderer.disableBracketedPaste();
        }

        if (mouseHandler != null) {
            mouseHandler.cleanupCursors(renderer);
        }

        disableMouse(renderer);

        if (renderer.reportFocus()) {
            renderer.disableReportFocus();
        }

        if (renderer.kittyKeyboard()) {
            renderer.disableKittyKeyboard();
        }

        if (renderer.altScreen()) {
            renderer.exitAltScreen();
        }

        isRunning.set(false);
        commandExecutor.shutdown();
        try {
            terminal.close();
        } catch (IOException e) {
            if (lastError != null) {
                e.addSuppressed(lastError);
            }
            throw new UncheckedIOException(e);
        } finally {
            if (programTerminal != null) {
                programTerminal.closeOpenedInput();
            }
        }
    }

    private static void disableMouse(Renderer renderer) {
        renderer.disableMouseSGRMode();
        renderer.disableMouseNormalTracking();
        renderer.disableMouseCellMotion();
        renderer.disableMouseAllMotion();
    }
}

