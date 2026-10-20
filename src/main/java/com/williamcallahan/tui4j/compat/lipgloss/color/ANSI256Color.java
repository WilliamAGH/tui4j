package com.williamcallahan.tui4j.compat.lipgloss.color;

import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import org.jline.utils.AttributedStyle;

/**
 * Port of Lip Gloss ansi 256 color.
 * Bubble Tea: bubbletea/examples/list-fancy/main.go
 */
public final class ANSI256Color implements TerminalColor, RGBSupplier {

    private final ColorCodeApplyStrategy applyStrategy;
    private final int colorCode;

    /**
     * Creates a 256-color entry from an ANSI color code.
     *
     * @param colorCode ANSI 256 color code
     */
    public ANSI256Color(int colorCode) {
        this.applyStrategy = new ColorCodeApplyStrategy(colorCode);
        this.colorCode = colorCode;
    }

    @Override
    public AttributedStyle applyAsBackground(AttributedStyle style, Renderer renderer) {
        return applyStrategy.applyForBackground(style);
    }

    @Override
    public AttributedStyle applyAsForeground(AttributedStyle style, Renderer renderer) {
        return applyStrategy.applyForForeground(style);
    }

    @Override
    public RGB rgb() {
        return RGB.fromHexString(ANSIColors.ANSI_HEX[colorCode]);
    }

    /**
     * Converts this 256-color entry to the closest ANSI 16-color entry.
     *
     * @return closest ANSI 16-color entry
     */
    public ANSIColor toANSIColor() {
        int ansiColorCode = 0;
        float minDistance = Float.MAX_VALUE;
        HSLuv source = HSLuv.fromRgb(RGB.fromHexString(ANSIColors.ANSI_HEX[colorCode]));

        for (int colorIndex = 0; colorIndex <= 15; colorIndex++) {
            RGB candidate = RGB.fromHexString(ANSIColors.ANSI_HEX[colorIndex]);
            float distance = (float) source.distanceTo(HSLuv.fromRgb(candidate));

            if (distance < minDistance) {
                minDistance = distance;
                ansiColorCode = colorIndex;
            }
        }
        return new ANSIColor(ansiColorCode);
    }

    /**
     * Returns the ANSI 256-color code.
     *
     * @return color code (0-255)
     */
    public int value() {
        return colorCode;
    }
}