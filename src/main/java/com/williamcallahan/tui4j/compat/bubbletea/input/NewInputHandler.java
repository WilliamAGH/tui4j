package com.williamcallahan.tui4j.compat.bubbletea.input;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.BlurMessage;
import com.williamcallahan.tui4j.compat.bubbletea.FocusMessage;
import com.williamcallahan.tui4j.compat.bubbletea.ProgramException;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UnknownSequenceMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.ExtendedSequences;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

/**
 * Port of Bubble Tea new input handler.
 * Bubble Tea: bubbletea/inputreader_other.go
 */
public class NewInputHandler implements InputHandler {
    private static final Pattern MOUSE_SGR_REGEX = Pattern.compile("(\\d+);(\\d+);(\\d+)([Mm])");
    private final Terminal terminal;
    private final Consumer<Message> messageConsumer;
    private volatile boolean running;
    private final ExecutorService inputExecutor;

    private static final int PEEK_TIMEOUT_MS = 10;
    private static final int BUFFER_SIZE = 256;

    private static final String BP_START = "\u001b[200~";
    private static final String BP_END = "\u001b[201~";

    private boolean inBracketedPaste = false;
    private final StringBuilder pasteBuffer = new StringBuilder();

    /**
     * Creates an input handler backed by a terminal reader.
     *
     * @param terminal terminal instance
     * @param messageConsumer message sink
     */
    public NewInputHandler(Terminal terminal, Consumer<Message> messageConsumer) {
        this.terminal = terminal;
        this.messageConsumer = messageConsumer;
        this.inputExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tui4j-Input-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void start() {
        if (!running) {
            running = true;
            inputExecutor.submit(this::handleInput);
        }
    }

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

    private void handleInput() {
        try {
            NonBlockingReader reader = terminal.reader();
            char[] buffer = new char[BUFFER_SIZE];
            char[] leftover = new char[0];

            while (running) {
                int numRead = reader.read(buffer, 0, BUFFER_SIZE);
                if (numRead < 0) {
                    break;
                }

                char[] inputChunk = Arrays.copyOfRange(buffer, 0, numRead);
                if (leftover.length > 0) {
                    inputChunk = InputChars.append(leftover, inputChunk);
                    leftover = new char[0];
                }

                int i = 0;
                while (i < inputChunk.length) {
                    int processed = processOneMessage(Arrays.copyOfRange(inputChunk, i, inputChunk.length));
                    if (processed == 0) {
                        leftover = InputChars.append(leftover, Arrays.copyOfRange(inputChunk, i, inputChunk.length));
                        break;
                    }
                    i += processed;
                }

            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                throw new ProgramException("Unable to initialize keyboard input", e);
            }
        }
    }

    private int processOneMessage(char[] input) throws IOException {
        if (input.length == 0)
            return 0;

        if (inBracketedPaste) {
            return processBracketedPaste(input);
        }

        if (input[0] == '\u001b') {
            return processEscapeSequence(input);
        }

        return processRegularCharacter(input[0]);
    }

    /**
     * Processes input while in bracketed paste mode, buffering content until the end tag.
     *
     * @param input remaining input characters
     * @return number of characters consumed
     */
    private int processBracketedPaste(char[] input) {
        // Check if we have the end tag at the beginning
        if (InputChars.startsWith(input, BP_END)) {
            inBracketedPaste = false;
            String content = pasteBuffer.toString();
            pasteBuffer.setLength(0);
            // Upstream delivers a paste as a KeyMsg with the Paste flag set, so its string
            // form is "[content]" and key bindings cannot match pasted text.
            messageConsumer.accept(new KeyPressMessage(
                new Key(KeyType.KeyRunes, content.toCharArray(), false, true)));
            return BP_END.length();
        }

        // Check if end tag is in the middle
        int endIdx = InputChars.indexOf(input, BP_END);
        if (endIdx != -1) {
            pasteBuffer.append(input, 0, endIdx);
            return endIdx; // Next iteration will hit the startsWith check
        }

        // If not found, buffer everything safely
        int safeLen = input.length;
        // Check for partial match at end to avoid splitting the tag
        for (int len = 1; len < BP_END.length(); len++) {
            if (InputChars.endsWith(input, BP_END.substring(0, len))) {
                safeLen = input.length - len;
                break;
            }
        }

        if (safeLen == 0 && input.length > 0) {
            // Only partial tag, wait for more data
            return 0;
        }

        pasteBuffer.append(input, 0, safeLen);
        return safeLen;
    }

    /**
     * Processes input beginning with ESC, handling escape sequences, Alt+key combos,
     * and standalone ESC.
     *
     * @param input remaining input characters starting with ESC
     * @return number of characters consumed
     * @throws IOException if peeking for additional input fails
     */
    private int processEscapeSequence(char[] input) throws IOException {
        if (input.length == 1) {
            // Peek to check if more data is available
            NonBlockingReader reader = terminal.reader();
            if (reader.peek(PEEK_TIMEOUT_MS) < 0) {
                // No data available, treat as standalone ESC
                messageConsumer.accept(new KeyPressMessage(new Key(KeyAliases.getKeyType(KeyAliases.KeyAlias.KeyEscape))));
                return 1;
            }

            // Buffer might be incomplete, wait for more data
            return 0;
        }

        // Detect bracketed paste start
        if (InputChars.startsWith(input, BP_START)) {
            inBracketedPaste = true;
            pasteBuffer.setLength(0);
            return BP_START.length();
        }

        // Check if it's a known control sequence
        if (input[1] == '[' || input[1] == 'O') {
            return processControlSequence(input);
        }

        // Try to match the full input as an extended sequence first
        Key key = ExtendedSequences.getKey(new String(input));
        if (key != null) {
            messageConsumer.accept(new KeyPressMessage(key));
            return input.length;
        }

        // ESC followed by a printable character is Alt+char, regardless of buffer length
        // This handles cases where additional input is buffered after the Alt sequence
        char altChar = input[1];
        if (altChar >= 0x20 && altChar < 0x7F) {
            messageConsumer.accept(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[] { altChar }, true)));
            return 2;
        }

        // Unknown sequence starting with ESC - treat ESC as standalone
        messageConsumer.accept(new KeyPressMessage(new Key(KeyAliases.getKeyType(KeyAliases.KeyAlias.KeyEscape))));
        return 1;
    }

    /**
     * Processes a single regular (non-escape) character press.
     *
     * @param firstChar the character to process
     * @return number of characters consumed (always 1)
     */
    private int processRegularCharacter(char firstChar) {
        Key key = ExtendedSequences.getKey(String.valueOf(firstChar));
        if (key != null) {
            messageConsumer.accept(new KeyPressMessage(key));
        } else {
            messageConsumer.accept(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[] { firstChar }, false)));
        }

        return 1;
    }

    private int processControlSequence(char[] input) {
        if (input.length < 2)
            return 0;
        char firstChar = input[1];

        if (firstChar == 'O') {
            if (input.length < 3)
                return 0; // Incomplete sequence
            char secondChar = input[2];
            Key key = ExtendedSequences.getKey("\u001bO" + secondChar);
            if (key != null) {
                messageConsumer.accept(new KeyPressMessage(key));
                return 3;
            }
        }

        if (firstChar == '[') {
            if (input.length < 3)
                return 0;
            char secondChar = input[2];

            // Focus & Blur Events
            if (secondChar == 'I') {
                messageConsumer.accept(new FocusMessage());
                return 3;
            } else if (secondChar == 'O') {
                messageConsumer.accept(new BlurMessage());
                return 3;
            }

            // X10 Mouse Event
            if (secondChar == 'M') {
                if (input.length < 6)
                    return 0; // Need button, col, row
                handleX10MouseEvent(Arrays.copyOfRange(input, 3, 6));
                return 6;
            }

            // SGR Mouse Event
            if (secondChar == '<') {
                int endIdx = findEndIndex(input, 3, 'M', 'm');
                if (endIdx == -1)
                    return 0;
                handleSGRMouseEvent(Arrays.copyOfRange(input, 3, endIdx + 1));
                return endIdx + 1;
            }

            // Arrow Keys and F5–F12
            StringBuilder sequence = new StringBuilder("\u001b[");
            for (int i = 2; i < input.length; i++) {
                sequence.append(input[i]);
                Key key = ExtendedSequences.getKey(sequence.toString());
                if (key != null) {
                    messageConsumer.accept(new KeyPressMessage(key));
                    return i + 1;
                }
                if (Character.isLetter(input[i]) || input[i] == '~') {
                    break;
                }
            }

            // If we reach here, it's an unrecognized sequence
            if (input.length > 5) { // Arbitrary limit to prevent infinite waiting
                messageConsumer.accept(new UnknownSequenceMessage(sequence.toString()));
                return sequence.length();
            }
        }

        return 0; // Incomplete sequence
    }

    private int findEndIndex(char[] input, int start, char... terminators) {
        for (int i = start; i < input.length; i++) {
            for (char term : terminators) {
                if (input[i] == term)
                    return i;
            }
        }
        return -1;
    }

    private void handleX10MouseEvent(char[] input) {
        if (input.length < 3)
            return;
        int button = input[0];
        int col = input[1] - 32;
        int row = input[2] - 32;

        messageConsumer.accept(MouseMessage.parseX10MouseEvent(col, row, button));
    }

    private void handleSGRMouseEvent(char[] input) {
        Matcher matcher = MOUSE_SGR_REGEX.matcher(new String(input));
        if (matcher.matches()) {
            int button = Integer.parseInt(matcher.group(1));
            int col = Integer.parseInt(matcher.group(2));
            int row = Integer.parseInt(matcher.group(3));
            boolean release = matcher.group(4).equals("m");

            messageConsumer.accept(MouseMessage.parseSGRMouseEvent(button, col, row, release));
        }
    }

}
