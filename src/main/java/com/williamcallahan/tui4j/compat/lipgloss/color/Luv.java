package com.williamcallahan.tui4j.compat.lipgloss.color;

/**
 * CIELUV color space conversion and blending.
 * <p>
 * Upstream: {@code lucasb-eyer/go-colorful} v1.2.0 {@code colors.go} ({@code Color.Luv},
 * {@code Color.BlendLuv}, {@code LuvToXyzWhiteRef}), which bubbles {@code progress}
 * uses to interpolate the colors of a gradient ramp.
 * <p>
 * L* is in {@code [0, 1]} and u* and v* are in about {@code [-1, 1]}, as upstream.
 * tui4j: src/main/java/com/williamcallahan/tui4j/compat/lipgloss/color/Luv.java
 */
final class Luv {

    /** D65 white reference, used by the default CIELUV conversions. */
    private static final double[] WHITE_REFERENCE = {0.95047, 1.0, 1.08883};

    /** Constant of the CIE L*u*v* lightness curve, {@code (29/3)^3}. */
    private static final double CIE_KAPPA = 903.2962962962963;

    /** Linear-light threshold of the CIE L*u*v* lightness curve, {@code (6/29)^3}. */
    private static final double CIE_EPSILON = 0.0088564516790356308;

    /** XYZ to linear RGB matrix. */
    private static final double[][] XYZ_TO_LINEAR_RGB = {
            {3.2409699419045214, -1.5373831775700935, -0.49861076029300328},
            {-0.96924363628087983, 1.8759675015077207, 0.041555057407175613},
            {0.055630079696993609, -0.20397695888897657, 1.0569715142428786},
    };

    /**
     * Prevents instantiation.
     */
    private Luv() {
    }

    /**
     * Blends two colors in CIELUV space.
     *
     * @param from color at the start of the ramp
     * @param to color at the end of the ramp
     * @param t interpolation factor, zero for {@code from} and one for {@code to}
     * @return blended color
     */
    static RGB blend(RGB from, RGB to, double t) {
        double[] start = fromRgb(from);
        double[] end = fromRgb(to);

        return toRgb(
                start[0] + t * (end[0] - start[0]),
                start[1] + t * (end[1] - start[1]),
                start[2] + t * (end[2] - start[2]));
    }

    /** Converts an sRGB color to its CIELUV coordinates. */
    private static double[] fromRgb(RGB rgb) {
        double red = linearize(rgb.r());
        double green = linearize(rgb.g());
        double blue = linearize(rgb.b());

        double x = 0.41239079926595948 * red + 0.35758433938387796 * green + 0.18048078840183429 * blue;
        double y = 0.21263900587151036 * red + 0.71516867876775593 * green + 0.072192315360733715 * blue;
        double z = 0.019330818715591851 * red + 0.11919477979462599 * green + 0.95053215224966058 * blue;

        double relative = y / WHITE_REFERENCE[1];
        double lightness = relative <= CIE_EPSILON
                ? relative * CIE_KAPPA / 100.0
                : 1.16 * Math.cbrt(relative) - 0.16;

        double[] referenceChromaticity = xyzToUv(WHITE_REFERENCE[0], WHITE_REFERENCE[1], WHITE_REFERENCE[2]);
        double[] chromaticity = xyzToUv(x, y, z);

        return new double[] {
                lightness,
                13.0 * lightness * (chromaticity[0] - referenceChromaticity[0]),
                13.0 * lightness * (chromaticity[1] - referenceChromaticity[1]),
        };
    }

    /** Converts CIELUV coordinates back to an sRGB color. */
    private static RGB toRgb(double lightness, double u, double v) {
        double y = lightness <= 0.08
                ? WHITE_REFERENCE[1] * lightness * 100.0 * Math.pow(3.0 / 29.0, 3.0)
                : WHITE_REFERENCE[1] * Math.pow((lightness + 0.16) / 1.16, 3.0);

        double[] referenceChromaticity = xyzToUv(WHITE_REFERENCE[0], WHITE_REFERENCE[1], WHITE_REFERENCE[2]);
        double x = 0.0;
        double z = 0.0;
        if (lightness != 0.0) {
            double chromaticityU = u / (13.0 * lightness) + referenceChromaticity[0];
            double chromaticityV = v / (13.0 * lightness) + referenceChromaticity[1];
            x = y * 9.0 * chromaticityU / (4.0 * chromaticityV);
            z = y * (12.0 - 3.0 * chromaticityU - 20.0 * chromaticityV) / (4.0 * chromaticityV);
        }

        return new RGB(
                (float) delinearize(dot(0, x, y, z)),
                (float) delinearize(dot(1, x, y, z)),
                (float) delinearize(dot(2, x, y, z)));
    }

    /** Returns the linear RGB channel from XYZ coordinates. */
    private static double dot(int channel, double x, double y, double z) {
        return XYZ_TO_LINEAR_RGB[channel][0] * x
                + XYZ_TO_LINEAR_RGB[channel][1] * y
                + XYZ_TO_LINEAR_RGB[channel][2] * z;
    }

    /** Returns the CIE L*u*v* chromaticity of a CIEXYZ color. */
    private static double[] xyzToUv(double x, double y, double z) {
        double denominator = x + 15.0 * y + 3.0 * z;
        if (denominator == 0.0) {
            return new double[] {0.0, 0.0};
        }
        return new double[] {4.0 * x / denominator, 9.0 * y / denominator};
    }

    /** Converts an sRGB channel to linear light. */
    private static double linearize(double value) {
        if (value <= 0.04045) {
            return value / 12.92;
        }
        return Math.pow((value + 0.055) / 1.055, 2.4);
    }

    /** Converts a linear light channel back to sRGB. */
    private static double delinearize(double value) {
        if (value <= 0.0031308) {
            return 12.92 * value;
        }
        return 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
    }
}
