package com.williamcallahan.tui4j.compat.x.ansi;

import com.williamcallahan.tui4j.compat.x.ansi.parser.Action;
import com.williamcallahan.tui4j.compat.x.ansi.parser.State;
import com.williamcallahan.tui4j.compat.x.ansi.parser.TransitionTable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * ANSI-aware word wrap implementation.
 * <p>
 * Port of {@code x/ansi/wrap.go}.
 */
public final class WordWrap {

    /**
     * Prevents instantiation.
     */
    private WordWrap() {
    }

    /**
     * Accumulator for wrapping state to reduce parameter count.
     */
    private static final class WrapAccumulator {
        private final ByteArrayOutputStream buffer;
        private final ByteArrayOutputStream word;
        private final ByteArrayOutputStream space;
        private int curWidth;
        private int spaceWidth;

        WrapAccumulator(int bufferSize) {
            this.buffer = new ByteArrayOutputStream(bufferSize);
            this.word = new ByteArrayOutputStream();
            this.space = new ByteArrayOutputStream();
            this.curWidth = 0;
            this.spaceWidth = 0;
        }

        /**
         * Flushes the pending space buffer to the output.
         *
         * @return updated width
         */
        int flushSpace() {
            if (space.size() == 0) {
                return curWidth;
            }
            buffer.writeBytes(space.toByteArray());
            curWidth += spaceWidth;
            space.reset();
            spaceWidth = 0;
            return curWidth;
        }

        /**
         * Flushes the pending word buffer to the output.
         *
         * @param wordLen word width
         * @return updated width
         */
        int flushWord(int wordLen) {
            if (word.size() == 0) {
                return curWidth;
            }
            flushSpace();
            buffer.writeBytes(word.toByteArray());
            word.reset();
            curWidth += wordLen;
            return curWidth;
        }

        /**
         * Flushes remaining space without exceeding limit.
         *
         * @param limit the wrap width limit
         */
        void flushSpaceIfFits(int limit) {
            if (curWidth + spaceWidth > limit) {
                curWidth = 0;
            } else {
                buffer.writeBytes(space.toByteArray());
            }
            space.reset();
            spaceWidth = 0;
        }

        /**
         * Resets the accumulator for a new line.
         */
        void resetLine() {
            curWidth = 0;
            space.reset();
            spaceWidth = 0;
        }

        /**
         * Writes a character to the space buffer.
         *
         * @param b byte to write
         * @param width display width
         */
        void writeSpace(byte b, int width) {
            space.write(b);
            spaceWidth += width;
        }

        /**
         * Writes bytes to the space buffer.
         *
         * @param bytes bytes to write
         * @param width display width
         */
        void writeSpaceBytes(byte[] bytes, int width) {
            space.writeBytes(bytes);
            spaceWidth += width;
        }

        /**
         * Writes a character to the main buffer.
         *
         * @param b byte to write
         * @param width display width
         */
        void writeBuffer(byte b, int width) {
            buffer.write(b);
            curWidth += width;
        }

        /**
         * Writes bytes to the main buffer.
         *
         * @param bytes bytes to write
         * @param width display width
         */
        void writeBufferBytes(byte[] bytes, int width) {
            buffer.writeBytes(bytes);
            curWidth += width;
        }

        /**
         * Writes a newline to the main buffer.
         */
        void writeNewline() {
            buffer.write('\n');
        }

        /**
         * Returns the current width.
         *
         * @return current width
         */
        int getCurWidth() {
            return curWidth;
        }

        /**
         * Returns the current space width.
         *
         * @return space width
         */
        int getSpaceWidth() {
            return spaceWidth;
        }

        /**
         * Returns the final wrapped string.
         *
         * @return wrapped string
         */
        String toResult() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Word-wraps a string using grapheme width.
     *
     * @param input input string
     * @param limit wrap width
     * @param breakpoints breakpoint characters
     * @return wrapped string
     */
    public static String wordWrap(String input, int limit, String breakpoints) {
        return wordWrapInternal(Method.GRAPHEME_WIDTH, input, limit, breakpoints);
    }

    /**
     * Word-wraps a string using wcwidth.
     *
     * @param input input string
     * @param limit wrap width
     * @param breakpoints breakpoint characters
     * @return wrapped string
     */
    public static String wordWrapWc(String input, int limit, String breakpoints) {
        return wordWrapInternal(Method.WC_WIDTH, input, limit, breakpoints);
    }

    /**
     * Word-wraps a string using the provided width calculation method.
     *
     * @param method width calculation method
     * @param input input string
     * @param limit wrap width
     * @param breakpoints breakpoint characters
     * @return wrapped string
     */
    private static String wordWrapInternal(Method method, String input, int limit, String breakpoints) {
        if (limit < 1) {
            return input;
        }

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        WrapAccumulator acc = new WrapAccumulator(bytes.length);
        int wordLen = 0;
        TransitionTable table = TransitionTable.get();
        State pstate = State.GROUND;

        int i = 0;
        while (i < bytes.length) {
            TransitionTable.Transition transition = table.transition(pstate, bytes[i]);
            State state = transition.state();
            Action action = transition.action();

            if (state == State.UTF8) {
                GraphemeCluster.Result result = GraphemeCluster.getFirstGraphemeCluster(bytes, i, method);
                if (result == null) {
                    break;
                }
                byte[] clusterBytes = result.clusterBytes();
                int width = result.width();
                i += clusterBytes.length;

                int codePoint = WrapSupport.firstCodePoint(clusterBytes);
                if (codePoint >= 0 && WrapSupport.isWhitespace(codePoint) && codePoint != WrapSupport.NON_BREAKING_SPACE) {
                    if (wordLen > 0) {
                        acc.flushWord(wordLen);
                        wordLen = 0;
                    }
                    acc.writeSpaceBytes(clusterBytes, width);
                } else if (WrapSupport.containsBreakpointCodePoint(codePoint, breakpoints)) {
                    acc.flushSpace();
                    if (wordLen > 0) {
                        acc.flushWord(wordLen);
                        wordLen = 0;
                    }
                    acc.writeBufferBytes(clusterBytes, width);
                } else {
                    acc.word.writeBytes(clusterBytes);
                    wordLen += width;
                    if (acc.getCurWidth() + acc.getSpaceWidth() + wordLen > limit && wordLen < limit) {
                        acc.writeNewline();
                        acc.resetLine();
                    }
                }

                pstate = State.GROUND;
                continue;
            }

            switch (action) {
                case PRINT, EXECUTE -> {
                    char ch = (char) (bytes[i] & 0xFF);
                    switch (ch) {
                        case '\n' -> {
                            if (wordLen == 0) {
                                acc.flushSpaceIfFits(limit);
                            }
                            acc.flushWord(wordLen);
                            wordLen = 0;
                            acc.writeNewline();
                            acc.resetLine();
                        }
                        default -> {
                            if (WrapSupport.isWhitespace(ch)) {
                                acc.flushWord(wordLen);
                                wordLen = 0;
                                acc.writeSpace(bytes[i], 1);
                            } else if (ch == '-' || containsBreakpointChar(ch, breakpoints)) {
                                acc.flushSpace();
                                acc.flushWord(wordLen);
                                wordLen = 0;
                                acc.writeBuffer(bytes[i], 1);
                            } else {
                                acc.word.write(bytes[i]);
                                wordLen++;
                                if (acc.getCurWidth() + acc.getSpaceWidth() + wordLen > limit && wordLen < limit) {
                                    acc.writeNewline();
                                    acc.resetLine();
                                }
                            }
                        }
                    }
                }
                default -> acc.word.write(bytes[i]);
            }

            if (pstate != State.UTF8) {
                pstate = state;
            }
            i++;
        }

        acc.flushWord(wordLen);

        return acc.toResult();
    }

    /**
     * Reports whether the character is a breakpoint character.
     *
     * @param ch character to test
     * @param breakpoints breakpoint string
     * @return true if the character is a breakpoint
     */
    private static boolean containsBreakpointChar(char ch, String breakpoints) {
        if (breakpoints == null || breakpoints.isEmpty()) {
            return false;
        }
        return breakpoints.indexOf(ch) >= 0;
    }
}
