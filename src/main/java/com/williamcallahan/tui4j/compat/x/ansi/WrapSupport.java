package com.williamcallahan.tui4j.compat.x.ansi;

import java.nio.charset.StandardCharsets;

import com.ibm.icu.lang.UCharacter;

/**
 * Shared helpers for ANSI-aware wrapping operations.
 * <p>
 * Port of {@code x/ansi/wrap.go} helpers.
 */
final class WrapSupport {

    static final int NON_BREAKING_SPACE = 0xA0;

    /**
     * Prevents instantiation.
     */
    private WrapSupport() {
    }

    /**
     * Returns the first Unicode code point for the given UTF-8 byte slice.
     *
     * @param bytes UTF-8 bytes for a single grapheme cluster
     * @return the first code point, or -1 if the bytes are empty
     */
    static int firstCodePoint(byte[] bytes) {
        if (bytes.length == 0) {
            return -1;
        }
        String cluster = new String(bytes, StandardCharsets.UTF_8);
        if (cluster.isEmpty()) {
            return -1;
        }
        return cluster.codePointAt(0);
    }

    /**
     * Reports whether the given code point is whitespace.
     * <p>
     * Upstream uses Go's {@code unicode.IsSpace}, which follows the Unicode
     * White_Space property. {@code UCharacter.isWhitespace} mirrors Java's class
     * and excludes U+0085 (NEL), which upstream treats as a breakable space.
     *
     * @param codePoint code point to test
     * @return true when whitespace
     */
    static boolean isWhitespace(int codePoint) {
        return UCharacter.isUWhiteSpace(codePoint);
    }

    /**
     * Reports whether any byte in the cluster appears in the breakpoint bytes.
     *
     * @param clusterBytes UTF-8 bytes for the grapheme cluster
     * @param breakpointsBytes UTF-8 bytes for breakpoint characters
     * @return true if any byte matches
     */
    static boolean containsAny(byte[] clusterBytes, byte[] breakpointsBytes) {
        if (clusterBytes.length == 0 || breakpointsBytes.length == 0) {
            return false;
        }
        for (byte clusterByte : clusterBytes) {
            for (byte breakpointByte : breakpointsBytes) {
                if (clusterByte == breakpointByte) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reports whether the given code point is contained in the breakpoint string.
     * This compares full code points rather than individual UTF-8 bytes,
     * avoiding false positives when multi-byte characters share byte prefixes.
     *
     * @param codePoint code point to test
     * @param breakpoints breakpoint string (may be null or empty)
     * @return true if the code point is a breakpoint character
     */
    static boolean containsBreakpointCodePoint(int codePoint, String breakpoints) {
        if (codePoint < 0 || breakpoints == null || breakpoints.isEmpty()) {
            return false;
        }
        return breakpoints.codePoints().anyMatch(cp -> cp == codePoint);
    }
}
