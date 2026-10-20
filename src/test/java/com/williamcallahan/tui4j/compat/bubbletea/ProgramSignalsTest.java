package com.williamcallahan.tui4j.compat.bubbletea;

import com.williamcallahan.tui4j.message.CheckWindowSizeMessage;
import com.williamcallahan.tui4j.runtime.CommandExecutor;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ProgramSignals} learns about resizes from the terminal, which is the only
 * source that exists on every platform: the JVM has no {@code WINCH} signal on Windows.
 */
class ProgramSignalsTest {

    private static final long TIMEOUT_SECONDS = 5;

    @Test
    void terminalResizeSignalRequestsTheWindowSize() throws Exception {
        ProgramConfiguration config = new ProgramConfiguration();
        config.setWithoutSignalHandler(true);
        CommandExecutor executor = new CommandExecutor();
        BlockingQueue<Message> messages = new LinkedBlockingQueue<>();

        try (Terminal terminal = TerminalBuilder.builder()
                .system(false)
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build()) {
            ProgramSignals.registerAll(config, terminal, executor, messages::add, error -> {
                throw new AssertionError(error);
            });

            assertThat(messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("the initial size request")
                    .isInstanceOf(CheckWindowSizeMessage.class);

            terminal.raise(Terminal.Signal.WINCH);

            assertThat(messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("the size request after the terminal was resized")
                    .isInstanceOf(CheckWindowSizeMessage.class);
        } finally {
            executor.shutdown();
        }
    }
}
