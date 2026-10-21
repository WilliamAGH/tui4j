package com.williamcallahan.tui4j.compat.lipgloss.color;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.within;

/**
 * Tests rgb.
 * Bubble Tea: bubbletea/examples/list-fancy/main.go
 */
class RGBTest {

    @ParameterizedTest
    @MethodSource("hexCodesAndExpectedInts")
    void test_ShouldDecodeHexCodeToCorrectValue(String hexValue, int expectedValue, String expectedErrorMessage) {
        if (expectedErrorMessage != null) {
            // when & then
            assertThatThrownBy(() -> RGB.fromHexString(hexValue))
                    .isInstanceOf(NumberFormatException.class)
                    .hasMessage(expectedErrorMessage);
        } else {
            // given
            RGB rgb = RGB.fromHexString(hexValue);

            // when
            int rgbInt = rgb.toInt();

            // then
            assertThat(rgbInt).isEqualTo(expectedValue);
        }
    }

    public static Stream<Arguments> hexCodesAndExpectedInts() {
        return Stream.of(
                Arguments.of("#FF0000", 0xFF0000, null),
                Arguments.of("#00F", 0x0000FF, null),
                Arguments.of("#6B50FF", 0x6B50FF, null),
                Arguments.of("invalid color", 0x0, "color: invalid color is not a hex-color")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHSLTestData")
    void test_HSLConversion(String hexColor, float[] expectedHSL, String expectedErrorMessage) {
        if (expectedErrorMessage != null) {
            // when & then
            assertThatThrownBy(() -> RGB.fromHexString(hexColor))
                    .isInstanceOf(NumberFormatException.class)
                    .hasMessage(expectedErrorMessage);
        } else {
            // given
            RGB color = RGB.fromHexString(hexColor);

            // when
            HSL hsl = color.toHSL();

            // then
            assertThat(hsl.h()).isCloseTo(expectedHSL[0], within(0.01f));
            assertThat(hsl.s()).isCloseTo(expectedHSL[1], within(0.01f));
            assertThat(hsl.l()).isCloseTo(expectedHSL[2], within(0.01f));
        }
    }

    static Stream<Arguments> provideHSLTestData() {
        return Stream.of(
                // primary colors
                Arguments.of("#FF0000", new float[]{0.0f, 1.0f, 0.5f}, null),    // red
                Arguments.of("#00FF00", new float[]{120.0f, 1.0f, 0.5f}, null),  // green
                Arguments.of("#0000FF", new float[]{240.0f, 1.0f, 0.5f}, null),  // blue

                // grayscale
                Arguments.of("#FFFFFF", new float[]{0.0f, 0.0f, 1.0f}, null),    // white
                Arguments.of("#000000", new float[]{0.0f, 0.0f, 0.0f}, null),    // black
                Arguments.of("#808080", new float[]{0.0f, 0.0f, 0.5f}, null),    // gray

                // error cases
                Arguments.of("#GGG", null, "color: #GGG is not a hex-color"),
                Arguments.of("invalid", null, "color: invalid is not a hex-color")
        );
    }

    @Test
    void test_ColorApplyStrategy() {
        // given
        RGB color = RGB.fromHexString("#FF00FF");

        // when
        ColorApplyStrategy strategy = color.asColorApplyStrategy();

        // then
        assertThat(strategy).isInstanceOf(RGBAApplyStrategy.class);
    }

    /**
     * Blending must happen in CIELUV, as upstream does, so a gradient passes
     * through the same colors.
     * <p>
     * Expected bytes are {@code go-colorful.Color.BlendLuv(...).Hex()}, which is
     * the color bubbles {@code progress} interpolates for a ramp.
     */
    @ParameterizedTest(name = "{0} to {1} at {2} blends to {3}")
    @MethodSource("blendLuvData")
    void test_ShouldBlendInCieluv(String fromHex, String toHex, double t, String expectedHex) {
        // given
        RGB from = RGB.fromHexString(fromHex);
        RGB to = RGB.fromHexString(toHex);

        // when
        RGB blended = from.blendLuv(to, t);

        // then
        assertThat(toHexString(blended)).isEqualTo(expectedHex);
    }

    static Stream<Arguments> blendLuvData() {
        return Stream.of(
                Arguments.of("#FF0000", "#00FF00", 0.0, "#ff0000"),
                Arguments.of("#FF0000", "#00FF00", 0.25, "#f36e00"),
                // A plain sRGB interpolation would pass through #808000 here.
                Arguments.of("#FF0000", "#00FF00", 0.5, "#daa400"),
                Arguments.of("#FF0000", "#00FF00", 0.75, "#acd300"),
                Arguments.of("#FF0000", "#00FF00", 1.0, "#00ff00"),
                Arguments.of("#5A56E0", "#EE6FF8", 0.25, "#8a5be4"),
                Arguments.of("#5A56E0", "#EE6FF8", 0.5, "#ae61ea"),
                Arguments.of("#000000", "#FFFFFF", 0.5, "#777777")
        );
    }

    private static String toHexString(RGB rgb) {
        return "#%02x%02x%02x".formatted(
                channel(rgb.r()), channel(rgb.g()), channel(rgb.b()));
    }

    private static int channel(float value) {
        return ((int) (value * 255.0 + 0.5)) & 0xFF;
    }

    /**
     * Pins pre-refactor conversion results so sharing CIELUV conversion cannot change
     * either white reference or a palette choice.
     */
    @ParameterizedTest(name = "{0} preserves conversion bits and palette entries")
    @MethodSource("conversionBitsData")
    void test_ShouldPreserveExactColorConversions(String hex, long hsluvDistanceBits,
            int distanceBits, int redBits, int greenBits, int blueBits, int ansi256, int ansi) {
        RGB color = RGB.fromHexString(hex);
        RGB reference = RGB.fromHexString("#51371d");
        RGB blended = color.blendLuv(reference, 0.375);

        // Math.pow and Math.cbrt may differ by an ulp across JVM intrinsics, so the pinned
        // values are compared within a tolerance far below any palette decision boundary.
        assertThat(HSLuv.fromRgb(color).distanceTo(HSLuv.fromRgb(reference)))
                .isCloseTo(Double.longBitsToDouble(hsluvDistanceBits), within(1e-9));
        assertThat(color.distanceHSLuv(reference)).isCloseTo(Float.intBitsToFloat(distanceBits), within(1e-6f));
        assertThat(blended.r()).isCloseTo(Float.intBitsToFloat(redBits), within(1e-6f));
        assertThat(blended.g()).isCloseTo(Float.intBitsToFloat(greenBits), within(1e-6f));
        assertThat(blended.b()).isCloseTo(Float.intBitsToFloat(blueBits), within(1e-6f));
        assertThat(color.toANSI256Color().value()).isEqualTo(ansi256);
        assertThat(new ANSI256Color(ansi256).toANSIColor().value()).isEqualTo(ansi);
    }

    /** Returns exact baseline outputs for black, white, chromatic and low-light colors. */
    private static Stream<Arguments> conversionBitsData() {
        return Stream.of(
                Arguments.of("#000000", 4605979442780565545L, 1063112499, 1041758388, 1035859447, 1024987261, 16, 0),
                Arguments.of("#ffffff", 4607696302837700421L, 1066310400, 1060782889, 1060068580, 1059620246, 231, 15),
                Arguments.of("#ff0000", 4602845472305966894L, 1057275024, 1061229206, 1043142877, 1034771680, 196, 9),
                Arguments.of("#00ff00", 4607561965939734502L, 1066060178, 1050910483, 1060217485, 1038791195, 46, 10),
                Arguments.of("#0000ff", 4612222991784618053L, 1074742015, 1049841810, 1044814588, 1058964844, 21, 12),
                Arguments.of("#808080", 4606052512304684237L, 1063248601, 1054721912, 1053307410, 1052380090, 102, 8),
                Arguments.of("#010203", 4611411100260754601L, 1073229749, 1041862371, 1036233371, 1026716813, 16, 0),
                Arguments.of("#51371d", 0L, 0, 1050845859, 1046273246, 1038674154, 58, 3),
                Arguments.of("#b7014b", 4603066595646884764L, 1057686898, 1058230007, 1042063321, 1047229935, 232, 0)
        );
    }
}
