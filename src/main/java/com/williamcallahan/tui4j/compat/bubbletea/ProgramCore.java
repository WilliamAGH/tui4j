package com.williamcallahan.tui4j.compat.bubbletea;

import com.williamcallahan.tui4j.compat.bubbletea.input.InputHandler;
import com.williamcallahan.tui4j.compat.bubbletea.render.NilRenderer;
import com.williamcallahan.tui4j.compat.bubbletea.render.Renderer;
import com.williamcallahan.tui4j.compat.bubbletea.render.StandardRenderer;
import com.williamcallahan.tui4j.runtime.CommandExecutor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jline.terminal.Terminal;

/**
 * Internal runtime for {@link Program}.
 * <p>
 * Holds mutable program state and implements lifecycle orchestration
 * while {@link Program} remains a stable public facade. Coordinates
 * terminal initialization, the event loop, signal handling, and cleanup.
 * <p>
 * Upstream: bubbletea/tea.go
 *
 * @see Program
 * @see ProgramConfiguration
 */
final class ProgramCore {

    private static final Logger logger = Logger.getLogger(
        ProgramCore.class.getName()
    );

    private final ProgramConfiguration config;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final CountDownLatch initLatch = new CountDownLatch(1);
    private final BlockingQueue<Message> messageQueue =
        new LinkedBlockingQueue<>();
    private final CommandExecutor commandExecutor = new CommandExecutor();
    private final AtomicReference<Model> currentModel;

    private Throwable lastError;
    private Terminal terminal;
    private Renderer renderer;
    private InputHandler inputHandler;

    private ProgramTerminal programTerminal;
    private ProgramMouseHandler mouseHandler;
    private ProgramProcessExecutor processExecutor;

    ProgramCore(Model initialModel, ProgramConfiguration config) {
        this.currentModel = new AtomicReference<>(initialModel);
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Initializes terminal/renderer state after options have been applied.
     */
    void initializeTerminal() {
        try {
            programTerminal = ProgramTerminal.initialize(config, this::send);
            terminal = programTerminal.terminal();
            inputHandler = programTerminal.inputHandler();

            renderer = config.isWithoutRenderer()
                ? new NilRenderer()
                : new StandardRenderer(
                    terminal,
                    config.normalizeFps(config.fps())
                );

            mouseHandler = new ProgramMouseHandler(
                config,
                terminal::getHeight,
                this::send
            );
            processExecutor = new ProgramProcessExecutor(
                terminal,
                programTerminal.terminalIsTty(),
                renderer,
                currentModel::get,
                this::send
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize terminal", e);
        }
    }

    /** Enables the alternate screen buffer on startup. */
    void withAltScreen() {
        config.setEnableAltScreen(true);
        if (renderer != null) {
            renderer.enterAltScreen();
        }
    }

    /** Enables focus/blur reporting on startup. */
    void withReportFocus() {
        config.setEnableReportFocus(true);
        if (renderer != null) {
            renderer.enableReportFocus();
        }
    }

    /** Enables Kitty keyboard protocol on startup. */
    void withKittyKeyboard() {
        config.setEnableKittyKeyboard(true);
        if (renderer != null) {
            renderer.enableKittyKeyboard();
        }
    }

    private void trackMouse(Terminal.MouseTracking tracking) {
        if (terminal == null) {
            return;
        }
        try {
            terminal.trackMouse(tracking);
        } catch (RuntimeException ignored) {
            // Best-effort: unsupported terminals keep the ANSI-only behavior.
        }
    }

    /** Enables mouse tracking for all motion events. */
    void withMouseAllMotion() {
        config.setEnableMouseAllMotion(true);
        config.setEnableMouseCellMotion(false);
        if (renderer != null) {
            renderer.enableMouseAllMotion();
            renderer.enableMouseSGRMode();
        }
    }

    /** Enables mouse tracking for cell motion events. */
    void withMouseCellMotion() {
        config.setEnableMouseCellMotion(true);
        config.setEnableMouseAllMotion(false);
        if (renderer != null) {
            renderer.enableMouseCellMotion();
            renderer.enableMouseSGRMode();
        }
    }

    /** Extends the active selection when a wheel event occurs during selection. */
    void withMouseSelectionExtendOnScroll() {
        config.setExtendSelectionOnScroll(true);
    }

    /** Enables selection auto-scroll with defaults. */
    void withMouseSelectionAutoScroll() {
        config.setExtendSelectionOnScroll(true);
        config.setSelectionAutoScrollEnabled(true);
        if (mouseHandler != null) {
            mouseHandler.enableSelectionAutoScroll();
        }
    }

    /** Enables selection auto-scroll with explicit configuration. */
    void withMouseSelectionAutoScroll(int edgeRows, int intervalMs) {
        config.setExtendSelectionOnScroll(true);
        config.setSelectionAutoScrollEnabled(true);
        config.setSelectionAutoScrollEdgeRows(edgeRows);
        config.setSelectionAutoScrollIntervalMs(intervalMs);
        if (mouseHandler != null) {
            mouseHandler.configureSelectionAutoScroll(edgeRows, intervalMs);
        }
    }

    /** Enables cursor changes during selection. */
    void withMouseSelectionCursor() {
        config.setManageMouseSelectionCursor(true);
    }

    /** Enables hover cursor changes over non-whitespace text. */
    void withMouseHoverTextCursor() {
        config.setHoverTextCursorEnabled(true);
    }

    /**
     * Enables mouse cursor changes based on
     * {@link com.williamcallahan.tui4j.input.MouseCursor} hints.
     */
    void withMouseTargetCursor() {
        config.setMouseTargetCursorEnabled(true);
    }

    /** Enables click message emission for press/release. */
    void withMouseClicks() {
        config.setMouseClicksEnabled(true);
    }

    /** Runs the program and blocks until quit, returning the final model. */
    Model runWithFinalModel() {
        return runInternal();
    }

    void run() {
        runInternal();
    }

    private Model runInternal() {
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Program is already running!");
        }

        ProgramSignals.registerAll(
            config,
            terminal,
            commandExecutor,
            this::send,
            this::sendError
        );
        if (mouseHandler != null) {
            mouseHandler.startAutoScroll();
        }

        inputHandler.start();
        applyStartupOptions();

        renderer.hideCursor();
        renderer.start();
        installCancelSignal();

        Model finalModel = currentModel.get();
        boolean renderFinalView = false;

        try {
            Command initCommand = currentModel.get().init();
            commandExecutor
                .executeIfPresent(initCommand, this::send, this::sendError)
                .thenRun(initLatch::countDown);

            renderer.write(currentModel.get().view());

            ProgramEventLoop.Result loopResult = new ProgramEventLoop(
                isRunning,
                messageQueue,
                config,
                renderer,
                terminal,
                currentModel,
                commandExecutor,
                mouseHandler,
                processExecutor,
                this::send,
                this::sendError
            ).run();

            finalModel = loopResult.finalModel();
            if (loopResult.error() != null) {
                lastError = loopResult.error();
            }
            renderFinalView = true;
        } catch (Exception e) {
            if (config.isWithoutCatchPanics()) {
                throw e;
            }
            lastError = e;
        } finally {
            ProgramCleanup.cleanup(
                renderFinalView,
                finalModel,
                inputHandler,
                renderer,
                mouseHandler,
                terminal,
                programTerminal,
                isRunning,
                commandExecutor,
                lastError
            );
        }

        if (lastError != null) {
            throw new ProgramException(lastError);
        }
        return finalModel;
    }

    private void sendError(Throwable error) {
        send(new ErrorMessage(error));
    }

    void send(Message msg) {
        if (isRunning.get() && msg != null && !messageQueue.offer(msg)) {
            logger.log(Level.WARNING, "Failed to enqueue message: {0}", msg);
        }
    }

    boolean isRunning() {
        return isRunning.get();
    }

    void waitForInit() {
        try {
            initLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProgramException(e);
        }
    }

    private void applyStartupOptions() {
        if (config.isEnableAltScreen() && !renderer.altScreen()) {
            renderer.enterAltScreen();
        }
        if (!config.isWithoutBracketedPaste()) {
            renderer.enableBracketedPaste();
        }
        if (config.isEnableMouseCellMotion()) {
            renderer.enableMouseCellMotion();
            renderer.enableMouseSGRMode();
            trackMouse(Terminal.MouseTracking.Normal);
        } else if (config.isEnableMouseAllMotion()) {
            renderer.enableMouseAllMotion();
            renderer.enableMouseSGRMode();
            trackMouse(Terminal.MouseTracking.Any);
        }
        if (config.isEnableReportFocus() && !renderer.reportFocus()) {
            renderer.enableReportFocus();
        }
        if (config.isEnableKittyKeyboard() && !renderer.kittyKeyboard()) {
            renderer.enableKittyKeyboard();
        }
    }

    private void installCancelSignal() {
        if (config.cancelSignal() == null) {
            return;
        }
        config.cancelSignal().whenComplete((result, error) ->
            send(new QuitMessage())
        );
    }
}
