package com.williamcallahan.tui4j.compat.lipgloss;

import java.util.BitSet;

/**
 * Rune styling utilities for lipgloss.
 * <p>
 * Port of github.com/charmbracelet/lipgloss/runes.go.
 */
public class Runes {

    /**
     * Creates a runes helper instance.
     */
    public Runes() {
    }

    /**
     * Styles the runes at the given indices with {@code matched} and every other
     * rune with {@code unmatched}, rendering each run of equally styled runes once.
     * <p>
     * Indices count code points like Go's {@code []rune}, so a supplementary
     * character such as an emoji is one index; out-of-range indices are ignored.
     *
     * @param input text to style
     * @param indices code point indices to style as matched
     * @param matched style for runes at {@code indices}
     * @param unmatched style for all other runes
     * @return styled text
     */
    public static String styleRunes(String input, int[] indices, Style matched, Style unmatched) {
        int[] runes = input.codePoints().toArray();
        BitSet matchedRunes = new BitSet(runes.length);
        for (int index : indices) {
            if (index >= 0 && index < runes.length) {
                matchedRunes.set(index);
            }
        }

        StringBuilder out = new StringBuilder();
        StringBuilder group = new StringBuilder();
        for (int i = 0; i < runes.length; i++) {
            group.appendCodePoint(runes[i]);

            boolean matches = matchedRunes.get(i);
            if (matches != matchedRunes.get(i + 1) || i == runes.length - 1) {
                out.append((matches ? matched : unmatched).render(group.toString()));
                group.setLength(0);
            }
        }
        return out.toString();
    }

}
