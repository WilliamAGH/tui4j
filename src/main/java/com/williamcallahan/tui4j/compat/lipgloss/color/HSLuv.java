package com.williamcallahan.tui4j.compat.lipgloss.color;

/**
 * HSLuv color-space values and distance metric used to pick the nearest palette entry.
 * <p>
 * Upstream: {@code lucasb-eyer/go-colorful} v1.2.0 {@code colors.go} and {@code hsluv.go}
 * ({@code Color.HSLuv}, {@code Color.DistanceHSLuv}), which {@code muesli/termenv} v0.15.2
 * uses in {@code Profile.Convert} to convert a color to an ANSI 256 or ANSI palette entry.
 * <p>
 * Hue is measured in degrees and saturation and lightness in {@code [0, 1]}, as in upstream.
 * tui4j: src/main/java/com/williamcallahan/tui4j/compat/lipgloss/color/HSLuv.java
 */
final class HSLuv {

    /** Rounded D65 white reference HSLuv is defined against. */
    private static final double[] WHITE_REFERENCE = {0.95045592705167, 1.0, 1.089057750759878};

    /** XYZ to linear RGB matrix, also used for the gamut bounds in HSLuv. */
    private static final double[][] XYZ_TO_LINEAR_RGB = {
            {3.2409699419045214, -1.5373831775700935, -0.49861076029300328},
            {-0.96924363628087983, 1.8759675015077207, 0.041555057407175613},
            {0.055630079696993609, -0.20397695888897657, 1.0569715142428786},
    };

    /** Constant of the CIE L*u*v* lightness curve, {@code (29/3)^3}. */
    private static final double CIE_KAPPA = 903.2962962962963;

    /** Linear-light threshold of the CIE L*u*v* lightness curve, {@code (6/29)^3}. */
    private static final double CIE_EPSILON = 0.0088564516790356308;

    /** Upper lightness bound of the HSLuv conversion, on the 0-100 scale upstream uses internally. */
    private static final double MAX_LIGHTNESS = 99.9999999;

    /** Lower lightness bound of the HSLuv conversion, on the 0-100 scale upstream uses internally. */
    private static final double MIN_LIGHTNESS = 0.00000001;

    private final double hue;
    private final double saturation;
    private final double lightness;

    /**
     * Creates HSLuv values.
     *
     * @param hue hue in degrees
     * @param saturation saturation in {@code [0, 1]}
     * @param lightness lightness in {@code [0, 1]}
     */
    private HSLuv(double hue, double saturation, double lightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.lightness = lightness;
    }

    /**
     * Converts an sRGB color to its HSLuv values.
     *
     * @param rgb color to convert
     * @return HSLuv values for the color
     */
    static HSLuv fromRgb(RGB rgb) {
        double red = linearize(rgb.r());
        double green = linearize(rgb.g());
        double blue = linearize(rgb.b());

        double x = 0.41239079926595948 * red + 0.35758433938387796 * green + 0.18048078840183429 * blue;
        double y = 0.21263900587151036 * red + 0.71516867876775593 * green + 0.072192315360733715 * blue;
        double z = 0.019330818715591851 * red + 0.11919477979462599 * green + 0.95053215224966058 * blue;

        double relativeLuminance = y / WHITE_REFERENCE[1];
        double luminance = relativeLuminance <= CIE_EPSILON
                ? relativeLuminance * CIE_KAPPA / 100.0
                : 1.16 * Math.cbrt(relativeLuminance) - 0.16;

        double[] chromaReference = xyzToUv(WHITE_REFERENCE[0], WHITE_REFERENCE[1], WHITE_REFERENCE[2]);
        double[] chroma = xyzToUv(x, y, z);
        double u = 13.0 * luminance * (chroma[0] - chromaReference[0]);
        double v = 13.0 * luminance * (chroma[1] - chromaReference[1]);

        return fromLuvLch(luminance, Math.sqrt(u * u + v * v), hueOf(u, v));
    }

    /**
     * Returns the Euclidean distance to another color in HSLuv space.
     * <p>
     * Upstream divides the hue difference by 100 so that hue, saturation, and
     * lightness carry comparable weight.
     *
     * @param other other HSLuv values
     * @return distance between the two colors
     */
    double distanceTo(HSLuv other) {
        double deltaHue = (hue - other.hue) / 100.0;
        double deltaSaturation = saturation - other.saturation;
        double deltaLightness = lightness - other.lightness;
        return Math.sqrt(deltaHue * deltaHue + deltaSaturation * deltaSaturation
                + deltaLightness * deltaLightness);
    }

    /** Converts CIE L*u*v* LCh values to HSLuv values, clamped as upstream does. */
    private static HSLuv fromLuvLch(double luminance, double chroma, double hue) {
        double scaledLightness = luminance * 100.0;
        double saturation = scaledLightness > MAX_LIGHTNESS || scaledLightness < MIN_LIGHTNESS
                ? 0.0
                : chroma * 100.0 / maxChromaForHue(scaledLightness, hue) * 100.0;

        return new HSLuv(hue, clamp01(saturation / 100.0), clamp01(scaledLightness / 100.0));
    }

    /**
     * Returns the hue in degrees, or zero when the color is achromatic.
     * <p>
     * Upstream guards against the floating-point noise of {@code atan2} for colors
     * whose chroma is almost zero.
     */
    private static double hueOf(double u, double v) {
        if (Math.abs(v - u) > 1e-4 && Math.abs(u) > 1e-4) {
            return (Math.toDegrees(Math.atan2(v, u)) + 360.0) % 360.0;
        }
        return 0.0;
    }

    /** Returns the largest in-gamut chroma for a lightness and hue. */
    private static double maxChromaForHue(double lightness, double hue) {
        double hueInRadians = hue / 360.0 * 2.0 * Math.PI;
        double minLength = Double.MAX_VALUE;
        for (double[] bound : gamutBounds(lightness)) {
            double length = lengthOfRayUntilIntersect(hueInRadians, bound[0], bound[1]);
            if (length > 0.0 && length < minLength) {
                minLength = length;
            }
        }
        return minLength;
    }

    /** Returns the sRGB gamut boundary lines for a lightness, as upstream computes them. */
    private static double[][] gamutBounds(double lightness) {
        double sub1 = Math.pow(lightness + 16.0, 3.0) / 1560896.0;
        double sub2 = sub1 > CIE_EPSILON ? sub1 : lightness / CIE_KAPPA;

        double[][] bounds = new double[6][2];
        for (int channel = 0; channel < 3; channel++) {
            for (int side = 0; side < 2; side++) {
                double top1 = (284517.0 * XYZ_TO_LINEAR_RGB[channel][0]
                        - 94839.0 * XYZ_TO_LINEAR_RGB[channel][2]) * sub2;
                double top2 = (838422.0 * XYZ_TO_LINEAR_RGB[channel][2]
                        + 769860.0 * XYZ_TO_LINEAR_RGB[channel][1]
                        + 731718.0 * XYZ_TO_LINEAR_RGB[channel][0]) * lightness * sub2
                        - 769860.0 * side * lightness;
                double bottom = (632260.0 * XYZ_TO_LINEAR_RGB[channel][2]
                        - 126452.0 * XYZ_TO_LINEAR_RGB[channel][1]) * sub2
                        + 126452.0 * side;

                bounds[channel * 2 + side][0] = top1 / bottom;
                bounds[channel * 2 + side][1] = top2 / bottom;
            }
        }
        return bounds;
    }

    /** Returns the length of a ray from the pole until it intersects a boundary line. */
    private static double lengthOfRayUntilIntersect(double theta, double slope, double intercept) {
        return intercept / (Math.sin(theta) - slope * Math.cos(theta));
    }

    /** Converts an sRGB channel to linear light. */
    private static double linearize(double value) {
        if (value <= 0.04045) {
            return value / 12.92;
        }
        return Math.pow((value + 0.055) / 1.055, 2.4);
    }

    /** Returns the CIE L*u*v* chromaticity of a CIEXYZ color. */
    private static double[] xyzToUv(double x, double y, double z) {
        double denominator = x + 15.0 * y + 3.0 * z;
        if (denominator == 0.0) {
            return new double[] {0.0, 0.0};
        }
        return new double[] {4.0 * x / denominator, 9.0 * y / denominator};
    }

    /** Clamps a value to the {@code [0, 1]} range, as upstream does. */
    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(value, 1.0));
    }
}
