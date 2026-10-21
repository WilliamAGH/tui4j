package com.williamcallahan.tui4j.compat.lipgloss;

import com.williamcallahan.tui4j.ansi.Truncate;

import java.util.Arrays;

/**
 * Applies a style's maximum width and height to a rendered block.
 * <p>
 * Port of the "Truncate according to MaxWidth" and "Truncate according to
 * MaxHeight" steps of {@code Style.Render}.
 * Upstream: lipgloss/style.go.
 * tui4j: src/main/java/com/williamcallahan/tui4j/compat/lipgloss/StyleSizeLimiter.java
 */
final class StyleSizeLimiter {

    /**
     * Prevents instantiation.
     */
    private StyleSizeLimiter() {
    }

    /**
     * Clamps a rendered block to the given size limits.
     * <p>
     * The block is split with a negative limit because Go's {@code strings.Split}
     * keeps trailing empty lines while Java's drops them, and a block ending in a
     * newline has a real final row that counts toward the height and gets
     * truncated with its own line.
     *
     * @param text rendered block
     * @param maxWidth maximum line width in cells, zero for unlimited
     * @param maxHeight maximum line count, zero for unlimited
     * @param ellipsis tail appended to a truncated line
     * @return block clamped to the limits
     */
    static String clamp(String text, int maxWidth, int maxHeight, String ellipsis) {
        String limited = text;
        if (maxWidth > 0) {
            limited = truncateLines(limited, maxWidth, ellipsis);
        }
        if (maxHeight > 0) {
            limited = limitHeight(limited, maxHeight);
        }
        return limited;
    }

    /** Truncates every line of the block to the maximum width. */
    private static String truncateLines(String text, int maxWidth, String ellipsis) {
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = Truncate.truncate(lines[i], maxWidth, ellipsis);
        }
        return String.join("\n", lines);
    }

    /** Keeps only the first lines of the block, up to the maximum height. */
    private static String limitHeight(String text, int maxHeight) {
        String[] lines = text.split("\n", -1);
        return String.join("\n", Arrays.copyOf(lines, Math.min(maxHeight, lines.length)));
    }
}
