package com.williamcallahan.tui4j.compat.bubbletea;

import com.williamcallahan.tui4j.message.CopyToClipboardMessage;
import com.williamcallahan.tui4j.message.OpenUrlMessage;
import com.williamcallahan.tui4j.message.ResetMouseCursorMessage;
import com.williamcallahan.tui4j.message.SetMouseCursorPointerMessage;
import com.williamcallahan.tui4j.message.SetMouseCursorTextMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests command.
 * tui4j: src/test/java/com/williamcallahan/tui4j/compat/bubbletea/CommandTest.java
 */
class CommandTest {

    @Test
    @DisplayName("Command.setMouseCursorText should return a SetMouseCursorTextMessage")
    void test_SetMouseCursorText() {
        Command cmd = Command.setMouseCursorText();
        Message msg = cmd.execute();
        assertThat(msg).isInstanceOf(SetMouseCursorTextMessage.class);
    }

    @Test
    @DisplayName("Command.setMouseCursorPointer should return a SetMouseCursorPointerMessage")
    void test_SetMouseCursorPointer() {
        Command cmd = Command.setMouseCursorPointer();
        Message msg = cmd.execute();
        assertThat(msg).isInstanceOf(SetMouseCursorPointerMessage.class);
    }

    @Test
    @DisplayName("Command.resetMouseCursor should return a ResetMouseCursorMessage")
    void test_ResetMouseCursor() {
        Command cmd = Command.resetMouseCursor();
        Message msg = cmd.execute();
        assertThat(msg).isInstanceOf(ResetMouseCursorMessage.class);
    }

    @Test
    @DisplayName("Command.copyToClipboard should return a CopyToClipboardMessage with correct text")
    void test_CopyToClipboard() {
        String text = "hello";
        Command cmd = Command.copyToClipboard(text);
        Message msg = cmd.execute();
        assertThat(msg).isInstanceOf(CopyToClipboardMessage.class);
        assertThat(((CopyToClipboardMessage) msg).text()).isEqualTo(text);
    }

    @Test
    @DisplayName("Command.openUrl should return an OpenUrlMessage with correct url")
    void test_OpenUrl() {
        String url = "https://example.com";
        Command cmd = Command.openUrl(url);
        Message msg = cmd.execute();
        assertThat(msg).isInstanceOf(OpenUrlMessage.class);
        assertThat(((OpenUrlMessage) msg).url()).isEqualTo(url);
    }

    /** Verifies {@link Command#none()} returns the NO_OP sentinel instance. */
    @Test
    @DisplayName("Command.none() returns the NO_OP sentinel")
    void test_NoneReturnsSentinel() {
        assertThat(Command.none()).isSameAs(Command.NO_OP);
    }

    /** Verifies executing the NO_OP sentinel yields null (no message). */
    @Test
    @DisplayName("Command.none() execute returns null")
    void test_NoneExecuteReturnsNull() {
        assertThat(Command.none().execute()).isNull();
    }

    /** Verifies null is recognized as absent by {@link Command#isNone}. */
    @Test
    @DisplayName("Command.isNone() returns true for null")
    void test_IsNoneNull() {
        assertThat(Command.isNone(null)).isTrue();
    }

    /** Verifies the NO_OP sentinel is recognized as absent by {@link Command#isNone}. */
    @Test
    @DisplayName("Command.isNone() returns true for Command.none()")
    void test_IsNoneForSentinel() {
        assertThat(Command.isNone(Command.none())).isTrue();
    }

    /** Verifies a real command is not mistaken for absent by {@link Command#isNone}. */
    @Test
    @DisplayName("Command.isNone() returns false for a real command")
    void test_IsNoneForRealCommand() {
        assertThat(Command.isNone(Command.quit())).isFalse();
    }

    /** Verifies {@link Command#batch} excludes null and NO_OP commands from the batch. */
    @Test
    @DisplayName("batch() filters out null and none commands")
    void test_BatchFiltersNoneCommands() {
        Command real = Command.quit();
        Command batched = Command.batch(real, Command.none(), null);
        Message msg = batched.execute();
        assertThat(msg).isInstanceOf(BatchMessage.class);
        assertThat(((BatchMessage) msg).commands()).containsExactly(real);
    }

    /**
     * Verifies {@link Command#println} joins its arguments the way upstream does.
     * <p>
     * Upstream: github.com/charmbracelet/bubbletea/standard_renderer.go {@code Println} builds the
     * body with {@code fmt.Sprint(args...)}, whose documented rule is "spaces are added between
     * operands when neither is a string". Verified against Go 1.26: {@code fmt.Sprint("a","b")}
     * is {@code "ab"}, {@code fmt.Sprint(1,2)} is {@code "1 2"}, {@code fmt.Sprint("a",1)} is
     * {@code "a1"}, {@code fmt.Sprint(1,"b")} is {@code "1b"}, {@code fmt.Sprint("x",1,2)} is
     * {@code "x1 2"}, and {@code fmt.Sprint()} is {@code ""}.
     */
    @Test
    @DisplayName("Command.println joins arguments like fmt.Sprint")
    void test_PrintlnJoinsArgumentsLikeFmtSprint() {
        assertThat(printLineBody(Command.println("a", "b"))).isEqualTo("ab");
        assertThat(printLineBody(Command.println(1, 2))).isEqualTo("1 2");
        assertThat(printLineBody(Command.println("a", 1))).isEqualTo("a1");
        assertThat(printLineBody(Command.println(1, "b"))).isEqualTo("1b");
        assertThat(printLineBody(Command.println("x", 1, 2))).isEqualTo("x1 2");
        assertThat(printLineBody(Command.println())).isEqualTo("");
        assertThat(printLineBody(Command.println("hello"))).isEqualTo("hello");
    }

    /**
     * Verifies the paste command reads the system clipboard itself and yields a
     * {@link PasteMessage} - or nothing when the clipboard is unavailable, which is what the
     * build's {@code tui4j.clipboard.disabled=true} produces. Upstream bubbles reads the local
     * clipboard inside {@code textinput.Paste}/{@code textarea.Paste} and returns the text as a
     * paste message; the port previously emitted an unhandled request message instead, so the
     * reply never arrived.
     */
    @Test
    @DisplayName("Command.paste yields no message when the clipboard is unavailable")
    void test_PasteCommandYieldsNoMessageWhenClipboardUnavailable() {
        assertThat(Command.paste().execute()).isNull();
    }

    /**
     * Executes a command and returns its print line body.
     *
     * @param command command expected to produce a {@link PrintLineMessage}
     * @return message body
     */
    private static String printLineBody(Command command) {
        Message message = command.execute();
        assertThat(message).isInstanceOf(PrintLineMessage.class);
        return ((PrintLineMessage) message).messageBody();
    }
}

