package com.williamcallahan.tui4j.compat.bubbletea;

import com.williamcallahan.tui4j.compat.bubbletea.input.InputHandler;
import com.williamcallahan.tui4j.compat.bubbletea.render.StandardRenderer;
import com.williamcallahan.tui4j.runtime.CommandExecutor;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the terminal writes emitted while a program shuts down.
 * <p>
 * Upstream: github.com/charmbracelet/bubbletea/tea.go {@code shutdown} runs
 * {@code renderer.stop()}, which erases the final row, then {@code restoreTerminalState}.
 * tui4j keeps the final row, so cleanup moves the cursor to the start of the next line
 * where the shell prompt appears.
 */
class ProgramCleanupTest {

    private final List<Object> puts = new ArrayList<>();
    private final StringWriter out = new StringWriter();

    private Terminal newRecordingTerminal() {
        PrintWriter writer = new PrintWriter(out);
        return (Terminal) Proxy.newProxyInstance(
            Terminal.class.getClassLoader(),
            new Class<?>[]{Terminal.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "writer" -> writer;
                case "flush" -> null;
                case "puts" -> {
                    puts.add(args[0]);
                    yield true;
                }
                case "getType" -> "xterm-256color";
                case "getWidth" -> 80;
                case "getHeight" -> 24;
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        if (returnType == char.class) return '\0';
        return null;
    }

    @Test
    @DisplayName("cleanup moves below the kept final row")
    void test_CleanupMovesBelowTheFinalRow() {
        Terminal terminal = newRecordingTerminal();
        StandardRenderer renderer = new StandardRenderer(terminal);
        InputHandler inputHandler = new InputHandler() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        };
        AtomicBoolean running = new AtomicBoolean(true);

        ProgramCleanup.cleanup(
            false,
            null,
            inputHandler,
            renderer,
            null,
            terminal,
            null,
            running,
            new CommandExecutor(),
            null
        );

        // Control: the cursor-visibility restore proves puts() calls are recorded.
        assertThat(puts).contains(InfoCmp.Capability.cursor_visible);
        assertThat(puts).containsSubsequence(
            InfoCmp.Capability.carriage_return, InfoCmp.Capability.cursor_down);
        assertThat(running).isFalse();
    }
}
