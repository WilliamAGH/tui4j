package com.williamcallahan.tui4j.compat.bubbletea.render;

import com.williamcallahan.tui4j.ansi.Code;
import com.williamcallahan.tui4j.compat.bubbletea.EnableMouseCellMotionMessage;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests standard renderer.
 * tui4j: src/test/java/com/williamcallahan/tui4j/compat/bubbletea/render/StandardRendererTest.java
 */
class StandardRendererTest {

    private static Terminal newFakeTerminal(StringWriter out) {
        PrintWriter writer = new PrintWriter(out);

        return (Terminal) Proxy.newProxyInstance(
                Terminal.class.getClassLoader(),
                new Class<?>[]{Terminal.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "writer" -> writer;
                        case "flush" -> null;
                        case "getType" -> "dumb";
                        case "getWidth" -> 80;
                        case "getHeight" -> 24;
                        case "puts" -> false;
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
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
        if (returnType == char.class) return (char) 0;
        return null;
    }

    @Test
    @DisplayName("Should write SetMouseTextCursor sequence when setMouseCursorText is called")
    void test_ShouldWriteSetMouseTextCursorSequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.setMouseCursorText();

        assertThat(out.toString()).isEqualTo(Code.SetMouseTextCursor.value());
    }

    @Test
    @DisplayName("Should write SetMousePointerCursor sequence when setMouseCursorPointer is called")
    void test_ShouldWriteSetMousePointerCursorSequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.setMouseCursorPointer();

        assertThat(out.toString()).isEqualTo(Code.SetMousePointerCursor.value());
    }

    @Test
    @DisplayName("Should write ResetMouseCursor sequence when resetMouseCursor is called")
    void test_ShouldWriteResetMouseCursorSequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.resetMouseCursor();

        assertThat(out.toString()).isEqualTo(Code.ResetMouseCursor.value());
    }

    @Test
    @DisplayName("Should write EnableMouseCellMotion sequence when enableMouseCellMotion is called")
    void test_ShouldWriteEnableMouseCellMotionSequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.enableMouseCellMotion();

        assertThat(out.toString()).isEqualTo(Code.EnableMouseCellMotion.value());
    }

    @Test
    @DisplayName("Should write DisableMouseCellMotion sequence when disableMouseCellMotion is called")
    void test_ShouldWriteDisableMouseCellMotionSequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.disableMouseCellMotion();

        assertThat(out.toString()).isEqualTo(Code.DisableMouseCellMotion.value());
    }

    @Test
    @DisplayName("Should write EnableMouseNormalTracking sequence when enableMouseNormalTracking is called")
    void test_ShouldWriteEnableMouseNormalTrackingSequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.enableMouseNormalTracking();

        assertThat(out.toString()).isEqualTo(Code.EnableMouseNormalTracking.value());
    }

    @Test
    @DisplayName("Should write DisableMouseNormalTracking sequence when disableMouseNormalTracking is called")
    void test_ShouldWriteDisableMouseNormalTrackingSequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.disableMouseNormalTracking();

        assertThat(out.toString()).isEqualTo(Code.DisableMouseNormalTracking.value());
    }

    @Test
    @DisplayName("Should write OSC 52 sequence when copyToClipboard is called")
    void test_ShouldWriteOsc52Sequence() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        String text = "hello world";
        renderer.copyToClipboard(text);

        assertThat(out.toString()).isEqualTo(Code.copyToClipboard(text));
    }

    /**
     * Verifies {@code stop()} renders the final frame, keeps its last row, and moves below it.
     * <p>
     * Upstream standard_renderer.go stop erases that row; tui4j keeps it so full-frame
     * views keep their bottom edge after exit, then starts the shell prompt on the next line.
     */
    @Test
    @DisplayName("stop renders the final frame and keeps its last row")
    void test_StopRendersFinalFrameAndKeepsLastRow() {
        StringWriter out = new StringWriter();
        Terminal terminal = newFakeTerminal(out);
        StandardRenderer renderer = new StandardRenderer(terminal);

        renderer.start();
        renderer.write("line one\nline two\nline three");
        renderer.stop();

        String output = out.toString();
        String afterFrame = output.substring(output.indexOf("line three"));
        assertThat(afterFrame).doesNotContain("\u001b[2K");
        assertThat(afterFrame).endsWith("\r\n");
    }

    /**
     * Verifies {@code stop()} adds no line advance for an alternate-screen frame or before any
     * frame was painted: the alternate screen restores the launch cursor on exit, and a program
     * that rendered nothing must not emit stray output.
     */
    @Test
    @DisplayName("stop adds no line advance in the alternate screen or before any frame")
    void test_StopAddsNoAdvanceWithoutNormalScreenFrame() {
        StringWriter unpainted = new StringWriter();
        new StandardRenderer(newFakeTerminal(unpainted)).stop();
        assertThat(unpainted.toString()).isEmpty();

        RendererTestTerminal alternate = new RendererTestTerminal(20, 5);
        StandardRenderer renderer = new StandardRenderer(alternate.terminal());
        renderer.start();
        renderer.enterAltScreen();
        renderer.write("alt frame");
        renderer.stop();
        assertThat(alternate.drain()).contains("alt frame").doesNotEndWith("\r\n");
    }

    /**
     * Verifies that enabling cell motion on a POSIX terminal emits only tui4j's own mode
     * sequences. JLine's POSIX {@code trackMouse} writes {@code ?1005h} and {@code ?1000h}, which
     * would replace the SGR encoding and downgrade cell motion to click-only tracking.
     */
    @Test
    @DisplayName("cell motion on a POSIX terminal keeps the SGR cell-motion sequences only")
    void test_CellMotionOnPosixTerminalKeepsSgrCellMotionOnly() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Terminal terminal = new DumbTerminal(
                "posix", "xterm-256color", new ByteArrayInputStream(new byte[0]), out, StandardCharsets.UTF_8)) {
            StandardRenderer renderer = new StandardRenderer(terminal);

            renderer.handleMessage(new EnableMouseCellMotionMessage());

            assertThat(out.toString(StandardCharsets.UTF_8))
                    .isEqualTo(Code.EnableMouseCellMotion.value() + Code.EnableMouseSgrExt.value());
        }
    }
}
