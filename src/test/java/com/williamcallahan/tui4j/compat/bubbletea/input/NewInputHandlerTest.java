package com.williamcallahan.tui4j.compat.bubbletea.input;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests bracketed-paste delivery from the input handler.
 * <p>
 * Upstream: github.com/charmbracelet/bubbletea/key_sequences.go {@code detectBracketedPaste} and
 * key.go - a paste arrives as a {@code KeyMsg{Type: KeyRunes, Runes: content, Paste: true}} whose
 * {@code String()} is {@code "[content]"}, so models and key bindings see it as a key event
 * (bubbles textinput inserts it through its default {@code msg.Runes} branch, and the brackets
 * keep bindings from matching pasted text).
 */
class NewInputHandlerTest {

    /** The ESC byte that opens every bracketed-paste envelope. */
    private static final char ESC = (char) 27;

    /**
     * Scripted reader mirroring {@link NonBlockingReader}'s buffered-peek contract: a peek moves
     * the next character from the source into the buffer, and the following read takes it.
     */
    private static final class ScriptedReader extends NonBlockingReader {

        private final char[] payload;
        private int position;
        private int buffered = -1;

        private ScriptedReader(String payload) {
            this.payload = payload.toCharArray();
        }

        @Override
        protected int read(long timeout, boolean bufferOnly) {
            if (bufferOnly) {
                if (buffered < 0) {
                    if (position >= payload.length) {
                        return EOF;
                    }
                    buffered = payload[position++];
                }
                return buffered;
            }
            if (buffered >= 0) {
                int ch = buffered;
                buffered = -1;
                return ch;
            }
            if (position >= payload.length) {
                return EOF;
            }
            return payload[position++];
        }

        @Override
        public int readBuffered(char[] cbuf, int off, int len, long timeout) throws IOException {
            int ch = read(timeout, false);
            if (ch == EOF) {
                return EOF;
            }
            cbuf[off] = (char) ch;
            int count = 1;
            while (count < len && position < payload.length) {
                cbuf[off + count] = payload[position++];
                count++;
            }
            return count;
        }

        @Override
        public void close() {
            // Nothing to release: the payload is an in-memory array.
        }
    }

    private static Terminal terminalReading(NonBlockingReader reader) {
        return (Terminal) Proxy.newProxyInstance(
            Terminal.class.getClassLoader(),
            new Class<?>[]{Terminal.class},
            (proxy, method, args) -> "reader".equals(method.getName())
                ? reader
                : defaultValue(method.getReturnType()));
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

    private static List<Message> collect(String payload) throws InterruptedException {
        List<Message> messages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        NewInputHandler handler = new NewInputHandler(
            terminalReading(new ScriptedReader(payload)),
            message -> {
                messages.add(message);
                latch.countDown();
            });
        try {
            handler.start();
            assertThat(latch.await(5, TimeUnit.SECONDS))
                .as("input handler produced a message")
                .isTrue();
        } finally {
            handler.stop();
        }
        return messages;
    }

    private static String describe(List<Message> messages) {
        StringBuilder out = new StringBuilder("[");
        for (Message message : messages) {
            if (out.length() > 1) {
                out.append(", ");
            }
            if (message instanceof KeyPressMessage keyPress) {
                out.append("KeyPress:").append(keyPress.key());
            } else if (message instanceof com.williamcallahan.tui4j.compat.bubbletea.UnknownSequenceMessage unknown) {
                out.append("Unknown:").append(unknown.sequence());
            } else {
                out.append(message.getClass().getSimpleName());
            }
        }
        return out.append(']').toString();
    }

    @Test
    @DisplayName("bracketed paste is delivered as a paste key message")
    void testBracketedPasteDeliversAPasteKey() throws InterruptedException {
        List<Message> messages = collect(ESC + "[200~hi there" + ESC + "[201~");

        assertThat(describe(messages)).isEqualTo("[KeyPress:[hi there]]");
    }

    @Test
    @DisplayName("a plain character still arrives as an ordinary key press")
    void testPlainCharacterIsAKeyPress() throws InterruptedException {
        List<Message> messages = collect("a");

        assertThat(describe(messages)).isEqualTo("[KeyPress:a]");
    }
}
