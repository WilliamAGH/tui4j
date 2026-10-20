package com.williamcallahan.tui4j.compat.x.ansi;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.BreakIterator;

import java.nio.charset.StandardCharsets;

/**
 * Grapheme cluster extraction and width calculation utilities.
 * Port of github.com/charmbracelet/x/ansi grapheme utilities.
 * <p>
 * Uses ICU4J's BreakIterator for Unicode-correct grapheme segmentation,
 * equivalent to Go's rivo/uniseg library used by the upstream implementation.
 * <p>
 * Methods return {@code null} when no cluster is found (empty input, end of sequence),
 * consistent with Go's nil return pattern. Callers should check for null before use.
 */
public final class GraphemeCluster {

    /** U+FE0F, the variation selector that requests an emoji (two-cell) glyph. */
    private static final char VARIATION_SELECTOR_16 = '\uFE0F';

    private GraphemeCluster() {}

    /**
     * Result of extracting the first grapheme cluster from a byte sequence.
     *
     * @param cluster the grapheme cluster as a UTF-8 string
     * @param clusterBytes the grapheme cluster as raw bytes
     * @param width the display width in cells
     */
    public record Result(String cluster, byte[] clusterBytes, int width) {}

    /**
     * Extracts the first grapheme cluster from a byte sequence starting at the given offset.
     * <p>
     * Returns {@code null} when:
     * <ul>
     *   <li>startIndex is beyond the byte array bounds</li>
     *   <li>The remaining bytes form an empty string</li>
     *   <li>No grapheme cluster can be extracted</li>
     * </ul>
     * This null return signals "no more clusters" and is consistent with Go's
     * rivo/uniseg.FirstGraphemeCluster which returns nil for empty input.
     *
     * @param bytes the byte sequence
     * @param startIndex the starting byte index
     * @param method the width calculation method
     * @return the grapheme cluster result, or {@code null} if no cluster available
     */
    public static Result getFirstGraphemeCluster(byte[] bytes, int startIndex, Method method) {
        if (startIndex >= bytes.length) {
            return null;
        }

        String text = new String(bytes, startIndex, bytes.length - startIndex, StandardCharsets.UTF_8);
        if (text.isEmpty()) {
            return null;
        }

        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(text);

        int clusterStart = iterator.first();
        int clusterEnd = iterator.next();

        if (clusterEnd == BreakIterator.DONE) {
            return null;
        }

        String cluster = text.substring(clusterStart, clusterEnd);
        int width = calculateWidth(cluster, method);
        byte[] clusterBytes = cluster.getBytes(StandardCharsets.UTF_8);

        return new Result(cluster, clusterBytes, width);
    }

    /**
     * Gets the first grapheme cluster from a string.
     *
     * @param s the input string
     * @param method the width calculation method
     * @return the cluster string and its width
     */
    public static StringResult getFirstGraphemeClusterString(String s, Method method) {
        if (s == null || s.isEmpty()) {
            return new StringResult("", 0);
        }

        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(s);

        int clusterStart = iterator.first();
        int clusterEnd = iterator.next();

        if (clusterEnd == BreakIterator.DONE) {
            return new StringResult("", 0);
        }

        String cluster = s.substring(clusterStart, clusterEnd);
        int width = calculateWidth(cluster, method);

        return new StringResult(cluster, width);
    }

    /**
     * Result of extracting a grapheme cluster from a string.
     *
     * @param cluster the grapheme cluster
     * @param width the display width in cells
     */
    public record StringResult(String cluster, int width) {}

    /**
     * Calculates the display width of a grapheme cluster.
     *
     * @param cluster the grapheme cluster
     * @param method the width calculation method
     * @return the display width in cells
     */
    public static int calculateWidth(String cluster, Method method) {
        if (cluster == null || cluster.isEmpty()) {
            return 0;
        }

        if (method == Method.WC_WIDTH) {
            return calculateWcWidth(cluster);
        }

        return calculateGraphemeWidth(cluster);
    }

    /**
     * WC_WIDTH: Traditional wcwidth behavior - sums per-code-point widths.
     */
    private static int calculateWcWidth(String cluster) {
        int width = 0;
        for (int i = 0; i < cluster.length(); ) {
            int codePoint = cluster.codePointAt(i);

            if (!isZeroWidth(codePoint) && Character.getType(codePoint) != Character.CONTROL) {
                width += isWideCharacter(codePoint) ? 2 : 1;
            }

            i += Character.charCount(codePoint);
        }
        return width;
    }

    /**
     * GRAPHEME_WIDTH: Treats the cluster as a single unit for width calculation.
     * <p>
     * U+FE0F requests the emoji glyph, which terminals draw in two cells even
     * when the base code point is East Asian Neutral (⚠️, ✔️, ⚙️, ❤️). The check
     * sits after the zero-width guard so that a cluster that segmenters split
     * after an ASCII base (the keycaps 1️⃣, #️⃣) is not counted twice.
     */
    private static int calculateGraphemeWidth(String cluster) {
        int codePoint = cluster.codePointAt(0);

        // Check for zero-width characters
        if (isZeroWidth(codePoint)) {
            return 0;
        }

        // Check for control characters
        if (Character.getType(codePoint) == Character.CONTROL) {
            return 0;
        }

        // Check for ZWJ sequences (family emojis, flags, etc.) - always wide
        if (cluster.length() > 1 && cluster.indexOf(Ansi.ZWJ_CHAR) >= 0) {
            return 2;
        }

        // U+FE0F selects the two-cell emoji glyph for an otherwise-narrow base.
        if (cluster.length() > 1 && cluster.indexOf(VARIATION_SELECTOR_16) >= 0) {
            return 2;
        }

        // Check if this is a wide character (emoji, CJK, or supplementary)
        if (isWideCharacter(codePoint) || isEmojiPresentation(codePoint)) {
            return 2;
        }

        return 1;
    }

    private static boolean isZeroWidth(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || codePoint == Ansi.ZERO_WIDTH_SPACE
                || codePoint == Ansi.ZERO_WIDTH_NON_JOINER
                || codePoint == Ansi.ZERO_WIDTH_JOINER
                || codePoint == Ansi.ZERO_WIDTH_NO_BREAK_SPACE;
    }

    /**
     * Determines if a code point occupies two terminal cells.
     * <p>
     * Uses the Unicode East Asian Width property instead of a hardcoded block
     * list: East Asian Wide and Fullwidth code points advance two columns, and
     * everything else (Neutral, Narrow, Halfwidth) advances one. Ambiguous code
     * points are rendered narrow in a non-East-Asian locale, so they advance
     * one as well.
     * <p>
     * A hardcoded block list cannot track this property: it under-reported wide
     * symbols outside its blocks (U+2705 ✅, U+274C ❌, U+2B50 ⭐ are Wide) so a
     * full-width row carrying one measured one cell short and wrapped the right
     * margin, and it over-reported narrow symbols inside its blocks (U+2600 ☀
     * and every supplementary code point are Neutral). Matches upstream x/ansi,
     * which delegates to go-runewidth's doublewidth table.
     *
     * @param codePoint code point to measure
     * @return {@code true} when the code point occupies two terminal cells
     */
    private static boolean isWideCharacter(int codePoint) {
        int eastAsianWidth = UCharacter.getIntPropertyValue(codePoint, UProperty.EAST_ASIAN_WIDTH);
        return eastAsianWidth == UCharacter.EastAsianWidth.WIDE
                || eastAsianWidth == UCharacter.EastAsianWidth.FULLWIDTH;
    }

    /**
     * Determines if a code point draws an emoji glyph by default.
     * <p>
     * Emoji presentation is two cells even when East Asian Width is Neutral:
     * a regional-indicator pair (🇺🇸) is one wide cluster, and the base of a
     * standalone emoji (U+1F1FA) draws wide too. Only the grapheme path consults
     * this; {@link Method#WC_WIDTH} sums per code point, so a regional-indicator
     * pair there is two one-cell runes, matching upstream.
     *
     * @param codePoint code point to measure
     * @return {@code true} when the code point has emoji presentation
     */
    private static boolean isEmojiPresentation(int codePoint) {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_PRESENTATION);
    }
}
