package com.williamcallahan.tui4j.input;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.OldLaggyInputHandler;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Tests the legacy Bubble Tea input handler port.
 * Bubble Tea: bubbletea/inputreader_other.go
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OldLaggyInputHandlerTest {

    @Mock
    private Terminal terminal;

    @Mock
    private NonBlockingReader reader;

    /**
     * Proves an intercepted Alt+key sequence does not mark the next unrelated key
     * press as Alt-modified.
     * <p>
     * The handler needs a lookahead read to resolve a non-CSI escape sequence, so
     * resolving {@code ESC a b} emits Alt+A while consuming {@code b}. The rune
     * press that follows must still arrive without the Alt modifier.
     */
    @Test
    void test_ShouldNotLeakAltModifier_AfterUnresolvedEscapeSequence() throws Exception {
        when(terminal.reader()).thenReturn(reader);
        List<Message> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);
        Consumer<Message> messageConsumer = message -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        };

        // ESC, then the following rune press, then end of input.
        when(reader.read())
                .thenReturn((int) '\u001b')
                .thenReturn((int) 'é')
                .thenReturn(-1);
        // Lookahead reads used to resolve the escape sequence.
        when(reader.read(anyLong()))
                .thenReturn((int) 'a')
                .thenReturn((int) 'b')
                .thenReturn(-1);

        OldLaggyInputHandler inputHandler = new OldLaggyInputHandler(terminal, messageConsumer);

        inputHandler.start();
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        inputHandler.stop();

        assertThat(received).isTrue();
        List<KeyPressMessage> keyPresses = new ArrayList<>();
        for (Message message : receivedMessages) {
            assertThat(message).isInstanceOf(KeyPressMessage.class);
            keyPresses.add((KeyPressMessage) message);
        }

        assertThat(keyPresses).hasSize(2);
        assertThat(keyPresses.getFirst().key()).isEqualTo("alt+a");
        assertThat(keyPresses.getFirst().alt()).isTrue();
        assertThat(keyPresses.get(1).key()).isEqualTo("é");
        assertThat(keyPresses.get(1).alt()).isFalse();
    }
}
