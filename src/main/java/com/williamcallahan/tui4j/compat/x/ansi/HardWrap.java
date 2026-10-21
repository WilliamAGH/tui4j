package com.williamcallahan.tui4j.compat.x.ansi;

import com.williamcallahan.tui4j.compat.x.ansi.parser.Action;
import com.williamcallahan.tui4j.compat.x.ansi.parser.State;
import com.williamcallahan.tui4j.compat.x.ansi.parser.TransitionTable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * ANSI-aware hard wrap implementation.
 * <p>
 * Port of {@code x/ansi/wrap.go}.
 */
public final class HardWrap {

    /**
     * Prevents instantiation.
     */
    private HardWrap() {
    }

    /**
     * Hard-wraps a string using grapheme width.
     *
     * @param input input string
     * @param limit wrap width
     * @param preserveSpace whether to preserve leading spaces
     * @return wrapped string
     */
    public static String hardWrap(String input, int limit, boolean preserveSpace) {
        return hardWrapInternal(Method.GRAPHEME_WIDTH, input, limit, preserveSpace);
    }

    /**
     * Hard-wraps a string using wcwidth.
     *
     * @param input input string
     * @param limit wrap width
     * @param preserveSpace whether to preserve leading spaces
     * @return wrapped string
     */
    public static String hardWrapWc(String input, int limit, boolean preserveSpace) {
        return hardWrapInternal(Method.WC_WIDTH, input, limit, preserveSpace);
    }

    /**
     * Hard-wraps a string using the provided width calculation method.
     *
     * @param method width calculation method
     * @param input input string
     * @param limit wrap width
     * @param preserveSpace whether to preserve leading spaces
     * @return wrapped string
     */
    private static String hardWrapInternal(Method method, String input, int limit, boolean preserveSpace) {
        if (limit < 1) {
            return input;
        }

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytes.length);
        TransitionTable table = TransitionTable.get();
        State pstate = State.GROUND;
        int curWidth = 0;
        boolean forceNewline = false;

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

                if (curWidth + width > limit) {
                    buffer.write('\n');
                    curWidth = 0;
                }

                if (!preserveSpace && curWidth == 0 && clusterBytes.length <= 4) {
                    int codePoint = WrapSupport.firstCodePoint(clusterBytes);
                    if (codePoint >= 0 && WrapSupport.isWhitespace(codePoint)) {
                        pstate = State.GROUND;
                        continue;
                    }
                }

                buffer.writeBytes(clusterBytes);
                curWidth += width;
                pstate = State.GROUND;
                continue;
            }

            switch (action) {
                case PRINT, EXECUTE -> {
                    byte b = bytes[i];
                    if (b == '\n') {
                        buffer.write('\n');
                        curWidth = 0;
                        forceNewline = false;
                        break;
                    }

                    if (curWidth + 1 > limit) {
                        buffer.write('\n');
                        curWidth = 0;
                        forceNewline = true;
                    }

                    if (curWidth == 0) {
                        if (!preserveSpace && forceNewline && WrapSupport.isWhitespace(b & 0xFF)) {
                            break;
                        }
                        forceNewline = false;
                    }

                    buffer.write(b);
                    if (action == Action.PRINT) {
                        curWidth++;
                    }
                }
                default -> buffer.write(bytes[i]);
            }

            if (pstate != State.UTF8) {
                pstate = state;
            }
            i++;
        }

        return buffer.toString(StandardCharsets.UTF_8);
    }
}
