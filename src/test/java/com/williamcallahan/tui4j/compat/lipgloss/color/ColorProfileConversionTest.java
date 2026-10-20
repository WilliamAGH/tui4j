package com.williamcallahan.tui4j.compat.lipgloss.color;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the ANSI profile conversions against the upstream palette choices.
 * <p>
 * Upstream: {@code muesli/termenv} v0.15.2 {@code profile.go} ({@code Profile.Convert}),
 * {@code color.go} ({@code hexToANSI256Color}, {@code ansi256ToANSIColor}), which select
 * the nearest palette entry by Euclidean distance in the HSLuv color space
 * ({@code lucasb-eyer/go-colorful} v1.2.0 {@code Color.DistanceHSLuv}).
 * <p>
 * The expected codes below are the codes termenv produces for the same inputs.
 */
class ColorProfileConversionTest {

    @ParameterizedTest(name = "{0} converts to ANSI 256 color {1}")
    @CsvSource({
            // Colors where a distance metric other than HSLuv selects another entry.
            "#51371d, 58",
            "#523e2a, 58",
            "#b7014b, 232",
            "#dd0b39, 232",
            "#de1246, 232",
            "#df1953, 232",
            "#e02060, 232",
            // Cube entries, gray ramp, and primaries that must keep their entry.
            "#000000, 16",
            "#ffffff, 231",
            "#111111, 232",
            "#444444, 59",
            "#eeeeee, 231",
            "#ff0000, 196",
            "#0000ff, 21",
            "#00ff00, 46"
    })
    void test_ShouldConvertRgbToTheNearestAnsi256Entry(String hex, int expectedColorCode) {
        // when
        TerminalColor converted = ColorProfile.ANSI256.convert(new RGBColor(hex));

        // then
        assertThat(converted).isInstanceOf(ANSI256Color.class);
        assertThat(((ANSI256Color) converted).value()).isEqualTo(expectedColorCode);
    }

    @ParameterizedTest(name = "ANSI 256 color {0} converts to ANSI color {1}")
    @CsvSource({
            // Entries where a distance metric other than HSLuv selects another entry.
            "24, 12",
            "25, 12",
            "29, 2",
            "32, 12",
            "33, 12",
            // Palette entries that must keep their entry.
            "0, 0",
            "16, 0",
            "46, 10",
            "58, 3",
            "196, 9",
            "231, 15",
            "232, 0",
            "255, 15"
    })
    void test_ShouldConvertAnsi256ToTheNearestAnsiEntry(int colorCode, int expectedColorCode) {
        // when
        TerminalColor converted = ColorProfile.ANSI.convert(new ANSI256Color(colorCode));

        // then
        assertThat(converted).isInstanceOf(ANSIColor.class);
        assertThat(((ANSIColor) converted).value()).isEqualTo(expectedColorCode);
    }

    @Test
    void test_ShouldReturnZeroDistanceForTheSameColor() {
        // given
        RGB color = RGB.fromHexString("#51371d");

        // when
        float distance = color.distanceHSLuv(RGB.fromHexString("#51371d"));

        // then
        assertThat(distance).isZero();
    }

    @Test
    void test_ShouldMeasureTheDistanceInTheHsluvColorSpace() {
        // #ff8700 converts to palette entry 208 because that entry equals the color.
        RGB orange = RGB.fromHexString("#ff8700");
        RGB exactEntry = RGB.fromHexString(ANSIColors.ANSI_HEX[208]);
        RGB distantGrayEntry = RGB.fromHexString(ANSIColors.ANSI_HEX[235]);

        // when
        float distanceToExactEntry = orange.distanceHSLuv(exactEntry);

        // then
        assertThat(distanceToExactEntry).isZero();
        assertThat(orange.distanceHSLuv(distantGrayEntry)).isGreaterThan(distanceToExactEntry);
    }
}
