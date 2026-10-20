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
}