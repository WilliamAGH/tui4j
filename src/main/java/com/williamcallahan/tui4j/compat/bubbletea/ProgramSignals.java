package com.williamcallahan.tui4j.compat.bubbletea;

import com.williamcallahan.tui4j.runtime.CommandExecutor;
import java.util.Objects;
import java.util.function.Consumer;
import org.jline.terminal.Terminal;
import org.jline.utils.Signals;

/**
 * OS signal registration for {@link Program}.
 * <p>
 * Extracted to keep {@link Program} focused on orchestration.
 * <p>
 * Resize notifications come from the {@link Terminal} instead of the OS: the JVM
 * has no {@code WINCH} signal on Windows, while JLine's system terminals raise
 * {@link Terminal.Signal#WINCH} when their size changes: POSIX ones from the OS signal
 * and the Windows console from its buffer-size events. Stream-backed terminals
 * (custom input/output) never raise it, because their size does not follow a window.
 * <p>
 * Upstream: bubbletea/signals_unix.go
 *
 * @see Program
 * @see ProgramConfiguration
 */
final class ProgramSignals {

    private ProgramSignals() {
    }

    /**
     * Registers all signals used by Program.
     *
     * @param config program configuration
     * @param terminal terminal that raises the resize signal
     * @param executor command executor
     * @param send message sink
     * @param sendError error sink
     */
    static void registerAll(
        ProgramConfiguration config,
        Terminal terminal,
        CommandExecutor executor,
        Consumer<Message> send,
        Consumer<Throwable> sendError
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(terminal, "terminal");
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(send, "send");
        Objects.requireNonNull(sendError, "sendError");

        registerTermination(config, executor, send, sendError);
        registerSuspend(config, executor, send, sendError);
        registerResize(terminal, executor, send, sendError);
    }

    private static void registerTermination(
        ProgramConfiguration config,
        CommandExecutor executor,
        Consumer<Message> send,
        Consumer<Throwable> sendError
    ) {
        if (config.isWithoutSignalHandler()) {
            return;
        }
        Signals.register("INT", () -> {
            if (config.ignoreSignals().get()) {
                return;
            }
            executor.executeIfPresent(QuitMessage::new, send, sendError);
        });
        Signals.register("TERM", () -> {
            if (config.ignoreSignals().get()) {
                return;
            }
            executor.executeIfPresent(QuitMessage::new, send, sendError);
        });
    }

    private static void registerSuspend(
        ProgramConfiguration config,
        CommandExecutor executor,
        Consumer<Message> send,
        Consumer<Throwable> sendError
    ) {
        if (config.isWithoutSignalHandler()) {
            return;
        }
        Signals.register("TSTP", () -> {
            if (config.ignoreSignals().get()) {
                return;
            }
            executor.executeIfPresent(SuspendMessage::new, send, sendError);
        });
        Signals.register("CONT", () -> {
            if (config.ignoreSignals().get()) {
                return;
            }
            executor.executeIfPresent(ResumeMessage::new, send, sendError);
        });
    }

    private static void registerResize(
        Terminal terminal,
        CommandExecutor executor,
        Consumer<Message> send,
        Consumer<Throwable> sendError
    ) {
        terminal.handle(Terminal.Signal.WINCH, signal ->
            executor.executeIfPresent(
                com.williamcallahan.tui4j.message.CheckWindowSizeMessage::new,
                send,
                sendError
            )
        );
        executor.executeIfPresent(
            com.williamcallahan.tui4j.message.CheckWindowSizeMessage::new,
            send,
            sendError
        );
    }
}

