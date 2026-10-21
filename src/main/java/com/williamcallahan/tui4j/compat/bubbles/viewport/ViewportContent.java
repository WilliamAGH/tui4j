package com.williamcallahan.tui4j.compat.bubbles.viewport;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.williamcallahan.tui4j.ansi.TextWidth;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.x.ansi.Cut;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Owns content measurement and rendering so navigation can remain in the viewport model.
 * Extracted from the port of {@code bubbles/viewport/viewport.go}.
 */
final class ViewportContent {

    private List<String> lines = new ArrayList<>();
    private int longestLineWidth;

    /**
     * Normalizes content and measures it once so horizontal bounds stay consistent.
     *
     * @param content the content to display, or {@code null} to clear it
     */
    void setContent(String content) {
        if (content == null || content.isEmpty()) {
            this.lines = new ArrayList<>();
        } else {
            String normalized = content.replace("\r\n", "\n");
            this.lines = new ArrayList<>(Arrays.asList(normalized.split("\n")));
        }
        this.longestLineWidth = findLongestLineWidth();
    }

    /**
     * Supplies the content length used by navigation bounds and progress.
     *
     * @return the number of content lines
     */
    int lineCount() {
        return lines.size();
    }

    /**
     * Supplies the measured cell width used to constrain horizontal movement.
     *
     * @return the longest content line's width in terminal cells
     */
    int longestLineWidth() {
        return longestLineWidth;
    }

    /**
     * Selects visible rows and preserves ANSI sequences when horizontally clipping them.
     *
     * @param width the viewport width including the style's horizontal frame
     * @param height the viewport height including the style's vertical frame
     * @param style the viewport style used to determine the available content area
     * @param xOffset the horizontal scroll offset in terminal cells
     * @param yOffset the vertical scroll offset in content lines
     * @return the visible content lines, clipped horizontally when needed
     */
    List<String> visibleLines(int width, int height, Style style, int xOffset, int yOffset) {
        int h = height - style.getVerticalFrameSize();
        int w = width - style.getHorizontalFrameSize();

        if (!lines.isEmpty()) {
            int top = max(0, yOffset);
            int low = min(top, lines.size());
            int high = max(top, lines.size());
            int bottom = max(low, min(high, yOffset + h));
            List<String> result = lines.subList(top, bottom);
            if ((xOffset == 0 && longestLineWidth <= w) || w <= 0) {
                return new ArrayList<>(result);
            }

            List<String> cutLines = new ArrayList<>();
            for (String line : result) {
                cutLines.add(Cut.cut(line, xOffset, xOffset + w));
            }
            return cutLines;
        }

        return new ArrayList<>();
    }

    /**
     * Pads the visible content before applying the viewport's existing style.
     * <p>
     * Rows are joined, not terminated, by newlines, as upstream {@code View} joins
     * {@code visibleLines()}: a trailing newline would render one row past the height.
     * As upstream, an inner style pads and caps the content to the area inside the
     * frame, so long lines never wrap into extra rows, and the caller's style is
     * applied to a copy with its size unset rather than mutated.
     *
     * @param width the viewport width including the style's horizontal frame
     * @param height the viewport height including the style's vertical frame
     * @param style the viewport style to size and apply to the visible content
     * @param xOffset the horizontal scroll offset in terminal cells
     * @param yOffset the vertical scroll offset in content lines
     * @return the styled viewport content
     */
    String view(int width, int height, Style style, int xOffset, int yOffset) {
        int effectiveWidth = max(1, width - style.getHorizontalFrameSize());
        int effectiveHeight = max(1, height - style.getVerticalFrameSize());

        List<String> rows = new ArrayList<>();
        for (String line : visibleLines(width, height, style, xOffset, yOffset)) {
            rows.add(line != null ? line : "");
        }

        String contents = Style.newStyle()
            .width(effectiveWidth)
            .height(effectiveHeight)
            .maxWidth(effectiveWidth)
            .maxHeight(effectiveHeight)
            .render(String.join("\n", rows));
        return style.copy().width(0).height(0).render(contents);
    }

    /**
     * Measures terminal cells rather than code units so wide characters retain their bounds.
     *
     * @return the greatest terminal cell width among the content lines, or zero if empty
     */
    private int findLongestLineWidth() {
        int w = 0;
        for (String line : lines) {
            int lineWidth = TextWidth.measureCellWidth(line != null ? line : "");
            if (lineWidth > w) {
                w = lineWidth;
            }
        }
        return w;
    }
}
