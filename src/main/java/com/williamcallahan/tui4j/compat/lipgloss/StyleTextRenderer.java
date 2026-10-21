package com.williamcallahan.tui4j.compat.lipgloss;

import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import org.jline.utils.AttributedCharSequence.ForceMode;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Applies a style's ANSI attributes to the lines of the text being styled.
 * <p>
 * Port of the "Render core text" step of {@code Style.Render}.
 * Upstream: lipgloss/style.go.
 * tui4j: src/main/java/com/williamcallahan/tui4j/compat/lipgloss/StyleTextRenderer.java
 */
final class StyleTextRenderer {

    /**
     * Prevents instantiation.
     */
    private StyleTextRenderer() {
    }

    /**
     * Renders every line of the text with the given attributes.
     * <p>
     * The text is split with a negative limit because Go's {@code strings.Split}
     * keeps trailing empty lines while Java's drops them, and a text ending in a
     * newline owns a real final blank line: dropping it removes that row and the
     * padding later steps would add to it.
     *
     * @param text text to render
     * @param style ANSI attributes to apply to every line
     * @param colorProfile color profile the renderer reports
     * @return text with the attributes applied to every line
     */
    static String render(String text, AttributedStyle style, ColorProfile colorProfile) {
        String[] lines = text.split("\n", -1);

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (colorProfile != ColorProfile.Ascii) {
                buffer.append(new AttributedString(lines[i], style).toAnsi(colorProfile.colorsCount(), ForceMode.None));
            } else {
                buffer.append(lines[i]);
            }
            if (i < lines.length - 1) {
                buffer.append('\n');
            }
        }
        return buffer.toString();
    }
}
