package com.williamcallahan.tui4j.compat.x.ansi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StringWidthTest {

    @Test
    @DisplayName("Should parse C1 CSI sequence and ignore it")
    void testC1CsiSequence() {
        // \u009B is CSI (Control Sequence Introducer) in C1
        // equivalent to ESC [
        String input = "\u009B31mHello\u009B0m";

        // Expected: 5 (CSI 31 m is a color code, invisible)
        int width = StringWidth.stringWidth(input);

        assertThat(width).as("Width should be 5 for input with C1 CSI sequence").isEqualTo(5);
    }

    @Test
    @DisplayName("C1 CSI sequences should match 7-bit ESC sequences")
    void testC1CsiSequenceMatchesEsc() {
        String c1Input = "\u009B31mHello\u009B0m";
        String escInput = "\u001B[31mHello\u001B[0m";

        assertThat(StringWidth.stringWidth(c1Input))
                .as("C1 CSI width should match ESC CSI width")
                .isEqualTo(StringWidth.stringWidth(escInput));
    }

    /**
     * A terminal draws an East Asian Wide or emoji-presentation glyph in two
     * cells. Measuring one leaves a row TUI4J believes fills the terminal one
     * cell short, so the row wraps the right margin and the renderer's cached
     * frame origin no longer matches the physical cursor.
     *
     * @param name case name
     * @param input grapheme cluster
     * @param expected terminal cells the cluster occupies
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("wideClusterData")
    @DisplayName("Grapheme width counts East Asian Wide and emoji presentation in two cells")
    void testGraphemeWidthIsTwoForWideAndEmojiPresentation(String name, String input, int expected) {
        assertThat(StringWidth.stringWidth(input))
                .as("%s should occupy %d cells", name, expected)
                .isEqualTo(expected);
    }

    private static Stream<Arguments> wideClusterData() {
        return Stream.of(
                // East Asian Width = Wide in the BMP: the block-list heuristic missed these.
                Arguments.of("check mark (U+2705)", "\u2705", 2),
                Arguments.of("cross mark (U+274C)", "\u274C", 2),
                Arguments.of("white medium star (U+2B50)", "\u2B50", 2),
                Arguments.of("high voltage (U+26A1)", "\u26A1", 2),
                Arguments.of("CJK ideograph (U+4F60)", "\u4F60", 2),
                Arguments.of("wave dash (U+301C)", "\u301C", 2),
                // East Asian Width = Fullwidth.
                Arguments.of("fullwidth vertical line (U+FF5C)", "\uFF5C", 2),
                // Emoji presentation, including supplementary pictographs and flags.
                Arguments.of("waving hand (U+1F44B)", "\uD83D\uDC4B", 2),
                Arguments.of("grinning face (U+1F600)", "\uD83D\uDE00", 2),
                Arguments.of("bubbles (U+1FAE7)", "\uD83E\uDEE7", 2),
                Arguments.of("flag (U+1F1FA U+1F1F8)", "\uD83C\uDDFA\uD83C\uDDF8", 2),
                Arguments.of("family ZWJ sequence", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66", 2),
                // U+FE0F requests the two-cell emoji glyph for a neutral base.
                Arguments.of("warning + VS16 (U+26A0 U+FE0F)", "\u26A0\uFE0F", 2),
                Arguments.of("check mark + VS16 (U+2714 U+FE0F)", "\u2714\uFE0F", 2),
                Arguments.of("gear + VS16 (U+2699 U+FE0F)", "\u2699\uFE0F", 2),
                // East Asian Width = Ambiguous or Neutral renders narrow outside East Asia.
                Arguments.of("circle half black (U+25D0, ambiguous)", "\u25D0", 1),
                Arguments.of("box drawing (U+2500, ambiguous)", "\u2500", 1),
                Arguments.of("horizontal ellipsis (U+2026, ambiguous)", "\u2026", 1),
                Arguments.of("circled digit one (U+2460, ambiguous)", "\u2460", 1),
                Arguments.of("sun (U+2600, neutral, text presentation)", "\u2600", 1),
                Arguments.of("braille spinner (U+280B, neutral)", "\u280B", 1),
                Arguments.of("latin small letter a", "a", 1)
        );
    }

    /**
     * WcWidth sums per code point, so the two regional indicators of a flag take
     * a column each while the grapheme path measures the flag glyph as one wide
     * cluster. Guards the split between the two methods.
     */
    @Test
    @DisplayName("WcWidth sums a regional-indicator pair per code point")
    void testWcWidthSumsRegionalIndicators() {
        String flag = "\uD83C\uDDFA\uD83C\uDDF8";

        assertThat(StringWidth.stringWidthWc(flag)).isEqualTo(2);
        assertThat(StringWidth.stringWidth(flag)).isEqualTo(2);
    }
}
