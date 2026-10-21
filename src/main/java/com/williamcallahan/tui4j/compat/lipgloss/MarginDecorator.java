package com.williamcallahan.tui4j.compat.lipgloss;

import static com.williamcallahan.tui4j.compat.lipgloss.PaddingDecorator.padLeft;
import static com.williamcallahan.tui4j.compat.lipgloss.PaddingDecorator.padRight;

import org.jline.utils.AttributedCharSequence.ForceMode;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Port of Lip Gloss margin rendering.
 * Upstream: lipgloss/style.go
 */
public class MarginDecorator {

    /**
     * Creates a utility container for margin rendering.
     */
    public MarginDecorator() {
    }

    /**
     * Applies margin padding to the provided input.
     *
     * @param input input content
     * @param topMargin top margin lines
     * @param rightMargin right margin columns
     * @param bottomMargin bottom margin lines
     * @param leftMargin left margin columns
     * @param attributedStyle optional style for margin space
     * @param renderer renderer for ANSI output
     * @return input content with margins applied
     */
    public static String applyMargins(
        String input,
        int topMargin,
        int rightMargin,
        int bottomMargin,
        int leftMargin,
        AttributedStyle attributedStyle,
        Renderer renderer
    ) {
        String padded = input;
        if (leftMargin > 0) {
            padded = padLeft(padded, leftMargin, attributedStyle, renderer);
        }
        if (rightMargin > 0) {
            padded = padRight(padded, rightMargin, attributedStyle, renderer);
        }

        int width = TextLines.fromText(padded).widestLineLength();
        String spaces = " ".repeat(width);
        if (attributedStyle != null) {
            spaces = new AttributedString(spaces, attributedStyle).toAnsi(
                renderer.colorProfile().colorsCount(),
                ForceMode.None
            );
        }
        if (topMargin > 0) {
            padded = (spaces + "\n").repeat(topMargin) + padded;
        }
        if (bottomMargin > 0) {
            padded += ("\n" + spaces).repeat(bottomMargin);
        }
        return padded;
    }
}
