package com.williamcallahan.tui4j.compat.bubbles.textarea;

import com.ibm.icu.lang.UCharacter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Word wrap for textarea input lines.
 * <p>
 * Port of {@code bubbles/textarea/textarea.go} {@code wrap}.
 * <p>
 * Two properties of the upstream grid matter to the rest of the component: a row
 * keeps the spaces that follow its last word instead of moving them to the start
 * of the next row, and the final row is padded with one extra space. The cursor
 * navigation and {@code LineInfo} measure rows with that trailing space, and the
 * renderer trims it when it would exceed the width.
 * tui4j: src/main/java/com/williamcallahan/tui4j/compat/bubbles/textarea/TextAreaWrap.java
 */
final class TextAreaWrap {

    private TextAreaWrap() {}

    /**
     * Wraps a line of runes to the given width.
     *
     * @param runes input runes
     * @param width wrap width
     * @return wrapped lines
     */
    static List<int[]> wrap(int[] runes, int width) {
        if (width <= 0) {
            return new ArrayList<>(List.of(Arrays.copyOf(runes, runes.length)));
        }

        List<int[]> lines = new ArrayList<>();
        lines.add(new int[0]);
        int[] word = new int[0];
        int row = 0;
        int spaces = 0;

        for (int rune : runes) {
            // Upstream uses Go's unicode.IsSpace, which follows the Unicode
            // White_Space property, so the ICU variant must be the property form
            // (UCharacter.isWhitespace mirrors Java's and excludes U+00A0).
            if (UCharacter.isUWhiteSpace(rune)) {
                spaces++;
            } else {
                word = TextAreaRunes.concat(word, new int[] {rune});
            }

            if (spaces > 0) {
                if (TextAreaRunes.cellWidth(lines.get(row)) + TextAreaRunes.cellWidth(word) + spaces > width) {
                    row++;
                    lines.add(TextAreaRunes.concat(word, spaces(spaces)));
                } else {
                    lines.set(row, TextAreaRunes.concat(
                            TextAreaRunes.concat(lines.get(row), word), spaces(spaces)));
                }
                spaces = 0;
                word = new int[0];
            } else {
                // The word buffer already holds the rune just read, so upstream
                // measures the last rune twice: a word that exactly fills the row
                // is closed there and a longer one is broken before that rune.
                int lastRuneWidth = TextAreaRunes.cellWidth(new int[] {word[word.length - 1]});
                if (TextAreaRunes.cellWidth(word) + lastRuneWidth > width) {
                    if (lines.get(row).length > 0) {
                        row++;
                        lines.add(new int[0]);
                    }
                    lines.set(row, TextAreaRunes.concat(lines.get(row), word));
                    word = new int[0];
                }
            }
        }

        // The trailing space at the end of the last row keeps navigation on the
        // final soft-wrapped row consistent with the rows above it.
        if (TextAreaRunes.cellWidth(lines.get(row)) + TextAreaRunes.cellWidth(word) + spaces >= width) {
            lines.add(TextAreaRunes.concat(word, spaces(spaces + 1)));
        } else {
            lines.set(row, TextAreaRunes.concat(
                    TextAreaRunes.concat(lines.get(row), word), spaces(spaces + 1)));
        }

        return lines;
    }

    /**
     * Returns a rune array of the given number of spaces.
     *
     * @param count number of spaces
     * @return space runes
     */
    private static int[] spaces(int count) {
        int[] result = new int[count];
        Arrays.fill(result, ' ');
        return result;
    }
}
