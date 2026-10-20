package com.williamcallahan.tui4j.input;

import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.InputHandler;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.NewInputHandler;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.BlurMessage;
import com.williamcallahan.tui4j.compat.bubbletea.FocusMessage;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UnknownSequenceMessage;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests the Bubble Tea input handler port.
 * Bubble Tea: bubbletea/inputreader_other.go
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InputHandlerTest {

    @Mock
    private Terminal terminal;

    @Mock
    private NonBlockingReader reader;

    @Test
    void test_ShouldPublishKeyPressMessage_WhenPressingRegularKey() throws Throwable {
        // given
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);
        Consumer<Message> messageConsumer = e -> {
            receivedMessages.add(e);
            messageLatch.countDown();
        };

        // Mocking reader.read(buffer, 0, BUFFER_SIZE)
        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = 'a';  // Simulate pressing 'a'
                    return 1;      // Number of characters read
                })
                .thenReturn(-1);   // Simulate end of input

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        // when
        inputHandler.start();
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);  // Allow time for processing
        inputHandler.stop();

        // then
        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(KeyPressMessage.class);

        KeyPressMessage keyMessage = (KeyPressMessage) receivedMessages.getFirst();

        // Correct key comparison
        assertThat(keyMessage.key()).isEqualTo("a");
        assertThat(keyMessage.alt()).isFalse();
    }

    @Test
    void test_ShouldRecognizeEscapeKey() throws Throwable {
        // given
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        // Mocking reader.read(buffer, 0, BUFFER_SIZE)
        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = '\u001b';  // Simulate pressing ESC
                    return 1;           // Number of characters read
                })
                .thenReturn(-1);         // Simulate end of input

        // ✅ Mocking peek to simulate no additional data after ESC
        when(reader.peek(10)).thenReturn(-1);

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        // when
        inputHandler.start();
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);  // Allow time for processing
        inputHandler.stop();

        // then
        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(KeyPressMessage.class);

        KeyPressMessage keyMessage = (KeyPressMessage) receivedMessages.getFirst();

        // ✅ Correct key comparison
        assertThat(keyMessage.type()).isEqualTo(KeyAliases.getKeyType(KeyAliases.KeyAlias.KeyEscape));
        assertThat(keyMessage.alt()).isFalse();
    }

    @Test
    void test_ShouldRecognizeAltKey() throws Throwable {
        // given
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        // ✅ Mocking reader.read(buffer, 0, BUFFER_SIZE)
        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = '\u001b';  // Simulate ESC (Alt modifier)
                    buf[1] = 'a';       // Simulate pressing 'a' after Alt
                    return 2;           // Two characters read
                })
                .thenReturn(-1);         // Simulate end of input

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        // when
        inputHandler.start();

        // Wait for message or timeout
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        inputHandler.stop();

        // then
        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(KeyPressMessage.class);

        KeyPressMessage keyMessage = (KeyPressMessage) receivedMessages.getFirst();
        assertThat(keyMessage.key()).isEqualTo("alt+a");
        assertThat(keyMessage.alt()).isTrue();
    }

    @Test
    void test_ShouldPublishPasteKey_WhenBracketedPasteReceived() throws Throwable {
        // given
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        String payload = "\u001b[200~hello\nworld\u001b[201~";

        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    payload.getChars(0, payload.length(), buf, 0);
                    return payload.length();
                })
                .thenReturn(-1);

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        // when
        inputHandler.start();

        // Wait for message or timeout
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        inputHandler.stop();

        // then
        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(KeyPressMessage.class);

        // Upstream key.go delivers a paste as a KeyMsg with the Paste flag: its string form is
        // "[content]" so key bindings cannot match pasted text, and the runes carry the content.
        KeyPressMessage pasteKey = (KeyPressMessage) receivedMessages.getFirst();
        assertThat(pasteKey.getKey().paste()).isTrue();
        assertThat(new String(pasteKey.runes())).isEqualTo("hello\nworld");
        assertThat(pasteKey.key()).isEqualTo("[hello\nworld]");
    }


    @Test
    void test_ShouldPublishFocusMessage_When_FocusGained() throws Throwable {
        // given
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        // ✅ Mocking reader.read(buffer, 0, BUFFER_SIZE)
        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = '\u001b';  // ESC
                    buf[1] = '[';       // '['
                    buf[2] = 'I';       // 'I' (Focus event)
                    return 3;           // Three characters read
                })
                .thenReturn(-1);         // Simulate end of input

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        // when
        inputHandler.start();

        // Wait for message or timeout
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        inputHandler.stop();

        // then
        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(FocusMessage.class);
    }

    @Test
    void test_ShouldPublishBlurMessage_When_FocusLost() throws Throwable {
        // given
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        // ✅ Mocking reader.read(buffer, 0, BUFFER_SIZE)
        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = '\u001b';  // ESC
                    buf[1] = '[';       // '['
                    buf[2] = 'O';       // 'O' (Blur event)
                    return 3;           // Three characters read
                })
                .thenReturn(-1);         // Simulate end of input

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        // when
        inputHandler.start();

        // Wait for message or timeout
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        inputHandler.stop();

        // then
        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(BlurMessage.class);
    }

    @Test
    void test_ShouldPublishMouseMessage_When_ReceivingX10MouseEvent() throws Throwable {
        // given
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = '\u001b';              // ESC
                    buf[1] = '[';                   // CSI
                    buf[2] = 'M';                   // Mouse Event Identifier
                    buf[3] = (char) (32 + 8);       // Button with Alt modifier
                    buf[4] = (char) (32 + 10);      // Column
                    buf[5] = (char) (32 + 20);      // Row
                    return 6;
                })
                .thenReturn(-1);  // Signal end of input

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        // when
        inputHandler.start();
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        inputHandler.stop();

        // then
        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(MouseMessage.class);

        MouseMessage mouseMessage = (MouseMessage) receivedMessages.getFirst();
        assertThat(mouseMessage.column()).isEqualTo(9);      // 10 - 1
        assertThat(mouseMessage.row()).isEqualTo(19);        // 20 - 1
        assertThat(mouseMessage.isAlt()).isTrue();
        assertThat(mouseMessage.isCtrl()).isFalse();
        assertThat(mouseMessage.isShift()).isFalse();
        assertThat(mouseMessage.getButton()).isEqualTo(MouseButton.MouseButtonLeft);
        assertThat(mouseMessage.getAction()).isEqualTo(MouseAction.MouseActionPress);
        assertThat(mouseMessage.isWheel()).isFalse();
    }

    @Test
    void test_ShouldPublishMouseMessage_When_ReceivingSGRMousePress() throws Throwable {
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = '\u001b';
                    buf[1] = '[';
                    buf[2] = '<';
                    buf[3] = '0';
                    buf[4] = ';';
                    buf[5] = '1';
                    buf[6] = '0';
                    buf[7] = ';';
                    buf[8] = '2';
                    buf[9] = '0';
                    buf[10] = 'M';
                    return 11;
                })
                .thenReturn(-1);

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        inputHandler.start();
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        inputHandler.stop();

        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(MouseMessage.class);

        MouseMessage mouseMessage = (MouseMessage) receivedMessages.getFirst();
        assertThat(mouseMessage.column()).isEqualTo(9);
        assertThat(mouseMessage.row()).isEqualTo(19);
        assertThat(mouseMessage.isAlt()).isFalse();
        assertThat(mouseMessage.isCtrl()).isFalse();
        assertThat(mouseMessage.isShift()).isFalse();
        assertThat(mouseMessage.getButton()).isEqualTo(MouseButton.MouseButtonLeft);
        assertThat(mouseMessage.getAction()).isEqualTo(MouseAction.MouseActionPress);
        assertThat(mouseMessage.isWheel()).isFalse();

        assertThat(mouseMessage.toString())
                .contains("width=9")
                .contains("height=19")
                .contains("shift=false")
                .contains("alt=false")
                .contains("ctrl=false")
                .contains("action=press")
                .contains("button=left");
    }

    @Test
    void test_ShouldPublishMouseMessage_When_ReceivingSGRMouseRelease() throws Throwable {
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    buf[0] = '\u001b';
                    buf[1] = '[';
                    buf[2] = '<';
                    buf[3] = '0';
                    buf[4] = ';';
                    buf[5] = '1';
                    buf[6] = '0';
                    buf[7] = ';';
                    buf[8] = '2';
                    buf[9] = '0';
                    buf[10] = 'm'; // 'm' for release (instead of 'M' for press)
                    return 11;
                })
                .thenReturn(-1);

        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        inputHandler.start();
        boolean received = messageLatch.await(1, TimeUnit.SECONDS);
        inputHandler.stop();

        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(MouseMessage.class);

        MouseMessage mouseMessage = (MouseMessage) receivedMessages.getFirst();
        assertThat(mouseMessage.column()).isEqualTo(9);
        assertThat(mouseMessage.row()).isEqualTo(19);
        assertThat(mouseMessage.isAlt()).isFalse();
        assertThat(mouseMessage.isCtrl()).isFalse();
        assertThat(mouseMessage.isShift()).isFalse();
        assertThat(mouseMessage.getButton()).isEqualTo(MouseButton.MouseButtonNone);
        assertThat(mouseMessage.getAction()).isEqualTo(MouseAction.MouseActionRelease);
        assertThat(mouseMessage.isWheel()).isFalse();

        assertThat(mouseMessage.toString())
                .contains("width=9")
                .contains("height=19")
                .contains("shift=false")
                .contains("alt=false")
                .contains("ctrl=false")
                .contains("action=release")
                .contains("button=none");
    }

    @Test
    void test_ShouldPublishUnknownSequenceMessage_When_ReceivingUnknownEscapeSequence() throws Throwable {
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };
        when(reader.read(any(char[].class), eq(0), eq(256)))
                .thenAnswer(invocation -> {
                    char[] buf = invocation.getArgument(0);
                    String unknownSequence = "\u001b[?999~";  // Clearly invalid sequence
                    for (int i = 0; i < unknownSequence.length(); i++) {
                        buf[i] = unknownSequence.charAt(i);
                    }
                    return unknownSequence.length();
                })
                .thenReturn(-1);


        InputHandler inputHandler = new NewInputHandler(terminal, messageConsumer);

        inputHandler.start();
        boolean received = messageLatch.await(1, TimeUnit.SECONDS);
        inputHandler.stop();

        assertThat(received).isTrue();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isInstanceOf(UnknownSequenceMessage.class);
    }

}
