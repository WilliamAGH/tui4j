package com.williamcallahan.tui4j.compat.bubbletea.input;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.ProgramException;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.ExtendedSequences;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbletea.BlurMessage;
import com.williamcallahan.tui4j.compat.bubbletea.FocusMessage;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UnknownSequenceMessage;

/**
 * Port of Bubble Tea old laggy input handler.
 * Bubble Tea: inputreader_other.go.
 */
public class OldLaggyInputHandler implements InputHandler {
    private static final Pattern MOUSE_SGR_REGEX = Pattern.compile("(\\d+);(\\d+);(\\d+)([Mm])");
    private final Terminal terminal;
    private final Consumer<Message> messageConsumer;
    private volatile boolean running;
    private final ExecutorService inputExecutor;

    // Buffer for handling escape sequences
    private static final int READ_TIMEOUT_MS = 50;

    /**
     * Creates OldLaggyInputHandler to keep this component ready for use.
     *
     * @param terminal terminal
     * @param messageConsumer message consumer
     */
    public OldLaggyInputHandler(Terminal terminal, Consumer<Message> messageConsumer) {
        this.terminal = terminal;
        this.messageConsumer = messageConsumer;
        this.inputExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tui4j-Input-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Handles start for this component.
     */
    @Override
    public void start() {
        if (!running) {
            running = true;
            inputExecutor.submit(this::handleInput);
        }
    }

    /**
     * Handles stop for this component.
     */
    @Override
    public void stop() {
        running = false;
        inputExecutor.shutdownNow();
        try {
            inputExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handles handle input for this component.
     */
    private void handleInput() {
        try {
            NonBlockingReader reader = terminal.reader();
            while (running) {
                int input = reader.read();
                if (input == -1)
                    break;

                if (input == '\u001b') { // ESC character
                    // Try to read the next character with a timeout
                    int nextChar = reader.read(READ_TIMEOUT_MS);
                    if (nextChar < 0) {
                        // If no character follows within timeout, it's a standalone ESC
                        messageConsumer
                                .accept(new KeyPressMessage(new Key(KeyAliases.getKeyType(KeyAliases.KeyAlias.KeyEscape))));
                    } else {
                        handleControlSequence(reader, (char) nextChar);
                    }
                    continue;
                }

                // Regular character press
                Key key = ExtendedSequences.getKey(String.valueOf((char) input));
                if (key != null) {
                    messageConsumer.accept(new KeyPressMessage(new Key(key.type(), key.runes())));
                } else {
                    messageConsumer
                            .accept(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[] { (char) input }, false)));
                }
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                throw new ProgramException("Unable to initialize keyboard input", e);
            }
        }
    }

    /**
     * Handles handle control sequence for this component.
     *
     * @param reader reader
     * @param firstChar first char
     */
    private void handleControlSequence(NonBlockingReader reader, char firstChar) throws IOException {
        StringBuilder sequence = new StringBuilder("\u001b");
        sequence.append(firstChar);

        int secondChar = reader.read(READ_TIMEOUT_MS);
        if (secondChar < 0) {
            Key key = ExtendedSequences.getKey(sequence.toString());
            if (key != null) {
                messageConsumer.accept(new KeyPressMessage(key));
            } else {
                messageConsumer.accept(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[] { firstChar }, true)));
            }
            return;
        }

        sequence.append((char) secondChar);

        if (firstChar != '[') {
            Key key = ExtendedSequences.getKey(sequence.toString());
            if (key != null) {
                messageConsumer.accept(new KeyPressMessage(new Key(key.type())));
            } else {
                messageConsumer.accept(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[] { firstChar }, true)));
            }
            return;
        }

        // Handle special cases based on secondChar
        switch (secondChar) {
            case 'I': // Focus event
                messageConsumer.accept(new FocusMessage());
                return;
            case 'O': // Blur event
                messageConsumer.accept(new BlurMessage());
                return;
            case 'M': // X10 Mouse Event
                handleX10MouseEvent(reader);
                return;
            case '<': // SGR Mouse Event
                handleSGRMouseEvent(reader);
                return;
            default:
                break;
        }

        readRemainingSequence(reader, sequence);
    }

    /**
     * Reads remaining characters of an unresolved CSI sequence and emits the resulting message.
     *
     * @param reader    terminal reader
     * @param sequence  sequence buffer built so far
     */
    private void readRemainingSequence(NonBlockingReader reader, StringBuilder sequence) throws IOException {
        while (true) {
            int ch = reader.read(READ_TIMEOUT_MS);
            if (ch <= 0)
                break;

            if (ch == 27) {
                emitKeyOrRunes(sequence);
                sequence = new StringBuilder();
            }
            sequence.append((char) ch);
        }

        Key key = ExtendedSequences.getKey(sequence.toString());
        if (key != null) {
            messageConsumer.accept(new KeyPressMessage(key));
        } else {
            messageConsumer.accept(new UnknownSequenceMessage(sequence.toString()));
        }
    }

    /**
     * Emits a key press for the given sequence, resolving via {@link ExtendedSequences} or falling back to runes.
     *
     * @param sequence escape sequence to resolve
     */
    private void emitKeyOrRunes(StringBuilder sequence) {
        Key key = ExtendedSequences.getKey(sequence.toString());
        if (key != null) {
            messageConsumer.accept(new KeyPressMessage(key));
        } else {
            messageConsumer.accept(
                    new KeyPressMessage(new Key(KeyType.KeyRunes, sequence.toString().toCharArray(), false)));
        }
    }

    /**
     * Handles handle x10 mouse event for this component.
     *
     * @param reader reader
     */
    private void handleX10MouseEvent(NonBlockingReader reader) throws IOException {
        // Read 3 bytes for button and coordinates
        int button = reader.read();
        int col = reader.read();
        int row = reader.read();

        messageConsumer.accept(MouseMessage.parseX10MouseEvent(col, row, button));
    }

    /**
     * Handles handle sgrmouse event for this component.
     *
     * @param reader reader
     */
    private void handleSGRMouseEvent(NonBlockingReader reader) throws IOException {
        StringBuilder buf = new StringBuilder();

        // Read until we find 'M' or 'm'
        int ch;
        while ((ch = reader.read(READ_TIMEOUT_MS)) != -1) {
            char c = (char) ch;
            buf.append(c);
            if (c == 'M' || c == 'm') {
                break;
            }
        }

        Matcher matcher = MOUSE_SGR_REGEX.matcher(buf.toString());
        if (matcher.matches()) {
            int button = Integer.parseInt(matcher.group(1));
            int col = Integer.parseInt(matcher.group(2));
            int row = Integer.parseInt(matcher.group(3));
            boolean release = matcher.group(4).equals("m");

            messageConsumer.accept(MouseMessage.parseSGRMouseEvent(button, col, row, release));
        }
    }
}
