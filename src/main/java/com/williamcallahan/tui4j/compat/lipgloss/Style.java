package com.williamcallahan.tui4j.compat.lipgloss;

import com.williamcallahan.tui4j.ansi.TextWrapper;
import com.williamcallahan.tui4j.compat.lipgloss.align.AlignmentDecorator;
import com.williamcallahan.tui4j.compat.lipgloss.border.Border;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.compat.lipgloss.color.TerminalColor;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer;

/**
 * Fluent styling and layout wrapper.
 * <p>
 * Port of `lipgloss/style.go`.
 * Provides a chainable API for coloring, sizing, padding, and aligning text content.
 */
public class Style {

    /**
     * Creates a new style using the default renderer.
     *
     * @return new style instance
     */
    public static Style newStyle() {
        return defaultRenderer.newStyle();
    }

    private final Renderer renderer;

    private String value;
    private Function<String, String> transformFunction;
    private TerminalColor background = new NoColor();
    private TerminalColor foreground = new NoColor();
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean blink;
    private boolean faint;
    private boolean reverse;
    private boolean inline;
    private int width;
    private int height;
    private int maxWidth;
    private int maxHeight;
    private String ellipsis = "";
    private Position horizontalAlign = Position.Left;
    private Position verticalAlign = Position.Top;

    private int topPadding;
    private int rightPadding;
    private int bottomPadding;
    private int leftPadding;

    private TerminalColor marginBackgroundColor = new NoColor();
    private int topMargin;
    private int rightMargin;
    private int bottomMargin;
    private int leftMargin;

    private Border borderDecoration;
    private boolean borderTop;
    private boolean borderRight;
    private boolean borderBottom;
    private boolean borderLeft;
    private boolean borderTopSet;
    private boolean borderRightSet;
    private boolean borderBottomSet;
    private boolean borderLeftSet;
    private TerminalColor borderTopForeground = new NoColor();
    private TerminalColor borderRightForeground = new NoColor();
    private TerminalColor borderBottomForeground = new NoColor();
    private TerminalColor borderLeftForeground = new NoColor();
    private TerminalColor borderTopBackground = new NoColor();
    private TerminalColor borderRightBackground = new NoColor();
    private TerminalColor borderBottomBackground = new NoColor();
    private TerminalColor borderLeftBackground = new NoColor();

    /**
     * Creates a style bound to the given renderer.
     *
     * @param renderer rendering context
     */
    public Style(Renderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Copy constructor that duplicates all styling properties from the source.
     * Protected to allow deprecated shim subclasses to support {@link #copy()}.
     *
     * @param source style to copy from
     */
    protected Style(Style source) {
        this.renderer = source.renderer;
        this.value = source.value;
        this.transformFunction = source.transformFunction;
        this.background = source.background;
        this.foreground = source.foreground;
        this.bold = source.bold;
        this.italic = source.italic;
        this.underline = source.underline;
        this.blink = source.blink;
        this.faint = source.faint;
        this.reverse = source.reverse;
        this.inline = source.inline;
        this.width = source.width;
        this.height = source.height;
        this.maxWidth = source.maxWidth;
        this.maxHeight = source.maxHeight;
        this.ellipsis = source.ellipsis;
        this.horizontalAlign = source.horizontalAlign;
        this.verticalAlign = source.verticalAlign;
        this.topPadding = source.topPadding;
        this.rightPadding = source.rightPadding;
        this.bottomPadding = source.bottomPadding;
        this.leftPadding = source.leftPadding;
        this.marginBackgroundColor = source.marginBackgroundColor;
        this.topMargin = source.topMargin;
        this.rightMargin = source.rightMargin;
        this.bottomMargin = source.bottomMargin;
        this.leftMargin = source.leftMargin;
        this.borderDecoration = source.borderDecoration;
        this.borderTop = source.borderTop;
        this.borderRight = source.borderRight;
        this.borderBottom = source.borderBottom;
        this.borderLeft = source.borderLeft;
        this.borderTopSet = source.borderTopSet;
        this.borderRightSet = source.borderRightSet;
        this.borderBottomSet = source.borderBottomSet;
        this.borderLeftSet = source.borderLeftSet;
        this.borderTopForeground = source.borderTopForeground;
        this.borderRightForeground = source.borderRightForeground;
        this.borderBottomForeground = source.borderBottomForeground;
        this.borderLeftForeground = source.borderLeftForeground;
        this.borderTopBackground = source.borderTopBackground;
        this.borderRightBackground = source.borderRightBackground;
        this.borderBottomBackground = source.borderBottomBackground;
        this.borderLeftBackground = source.borderLeftBackground;
    }

    /**
     * Sets the base string content.
     *
     * @param strings strings to join
     * @return this style
     */
    public Style setString(String... strings) {
        this.value = String.join(" ", strings);
        return this;
    }

    /**
     * Sets the foreground color.
     *
     * @param color foreground color
     * @return this style
     */
    public Style foreground(TerminalColor color) {
        this.foreground = color;
        return this;
    }

    /**
     * Sets the background color.
     *
     * @param color background color
     * @return this style
     */
    public Style background(TerminalColor color) {
        this.background = color;
        return this;
    }

    /**
     * Sets the bold attribute.
     *
     * @param bold bold flag
     * @return this style
     */
    public Style bold(boolean bold) {
        this.bold = bold;
        return this;
    }

    /**
     * Sets the italic attribute.
     *
     * @param italic italic flag
     * @return this style
     */
    public Style italic(boolean italic) {
        this.italic = italic;
        return this;
    }

    /**
     * Sets the underline attribute.
     *
     * @param underline underline flag
     * @return this style
     */
    public Style underline(boolean underline) {
        this.underline = underline;
        return this;
    }

    /**
     * Sets the reverse video attribute.
     *
     * @param reverse reverse flag
     * @return this style
     */
    public Style reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    /**
     * Sets the blink attribute.
     *
     * @param blink blink flag
     * @return this style
     */
    public Style blink(boolean blink) {
        this.blink = blink;
        return this;
    }

    /**
     * Sets the faint (dim) attribute.
     *
     * @param faint faint flag
     * @return this style
     */
    public Style faint(boolean faint) {
        this.faint = faint;
        return this;
    }

    /**
     * Sets whether to render on a single line.
     *
     * @param inline inline flag
     * @return this style
     */
    public Style inline(boolean inline) {
        this.inline = inline;
        return this;
    }

    /**
     * Sets the content width.
     *
     * @param width width in cells
     * @return this style
     */
    public Style width(int width) {
        this.width = width;
        return this;
    }

    /**
     * Returns the configured width for this style.
     *
     * @return the width, or {@code 0} when unset
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the content height.
     *
     * @param height height in lines
     * @return this style
     */
    public Style height(int height) {
        this.height = height;
        return this;
    }

    /**
     * Returns the configured height for this style.
     *
     * @return the height, or {@code 0} when unset
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the maximum content width for truncation.
     *
     * @param maxWidth maximum width
     * @return this style
     */
    public Style maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /**
     * Sets the maximum content height for truncation.
     *
     * @param maxHeight maximum height
     * @return this style
     */
    public Style maxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    /**
     * Sets the ellipsis string used when truncating.
     *
     * @param ellipsis ellipsis text
     * @return this style
     */
    public Style ellipsis(String ellipsis) {
        this.ellipsis = ellipsis;
        return this;
    }

    /**
     * Sets horizontal and vertical alignment.
     *
     * @param positions alignment positions (horizontal, then vertical)
     * @return this style
     */
    public Style align(Position... positions) {
        if (positions.length > 0) {
            this.horizontalAlign = positions[0];
        }
        if (positions.length > 1) {
            this.verticalAlign = positions[1];
        }
        return this;
    }

    /**
     * Sets the horizontal alignment.
     *
     * @param position horizontal position
     * @return this style
     */
    public Style alignHorizontal(Position position) {
        this.horizontalAlign = position;
        return this;
    }

    /**
     * Sets the vertical alignment.
     *
     * @param position vertical position
     * @return this style
     */
    public Style alignVertical(Position position) {
        this.verticalAlign = position;
        return this;
    }

    /**
     * Returns the maximum width setting.
     *
     * @return maximum width
     */
    public int getMaxWidth() {
        return maxWidth;
    }

    /**
     * Returns the maximum height setting.
     *
     * @return maximum height
     */
    public int getMaxHeight() {
        return maxHeight;
    }

    /**
     * Resets the maximum width to unlimited.
     *
     * @return this style
     */
    public Style unsetMaxWidth() {
        this.maxWidth = 0;
        return this;
    }

    /**
     * Resets the maximum height to unlimited.
     *
     * @return this style
     */
    public Style unsetMaxHeight() {
        this.maxHeight = 0;
        return this;
    }

    /**
     * Sets padding using CSS-like shorthand (top, right, bottom, left).
     *
     * @param values padding values
     * @return this style
     */
    public Style padding(int... values) {
        int[] boxValues = expandBoxValues(IntStream.range(0, values.length).toArray());
        this.topPadding = values[boxValues[0]];
        this.rightPadding = values[boxValues[1]];
        this.bottomPadding = values[boxValues[2]];
        this.leftPadding = values[boxValues[3]];
        return this;
    }

    /**
     * Sets top padding.
     *
     * @param topPadding top padding
     * @return this style
     */
    public Style paddingTop(int topPadding) {
        this.topPadding = topPadding;
        return this;
    }

    /**
     * Sets right padding.
     *
     * @param rightPadding right padding
     * @return this style
     */
    public Style paddingRight(int rightPadding) {
        this.rightPadding = rightPadding;
        return this;
    }

    /**
     * Returns the right padding.
     *
     * @return right padding
     */
    public int rightPadding() {
        return rightPadding;
    }

    /**
     * Sets bottom padding.
     *
     * @param bottomPadding bottom padding
     * @return this style
     */
    public Style paddingBottom(int bottomPadding) {
        this.bottomPadding = bottomPadding;
        return this;
    }

    /**
     * Sets left padding.
     *
     * @param leftPadding left padding
     * @return this style
     */
    public Style paddingLeft(int leftPadding) {
        this.leftPadding = leftPadding;
        return this;
    }

    /**
     * Returns the left padding.
     *
     * @return left padding
     */
    public int leftPadding() {
        return leftPadding;
    }

    /**
     * Sets margins using CSS-like shorthand (top, right, bottom, left).
     *
     * @param values margin values
     * @return this style
     */
    public Style margin(int... values) {
        int[] boxValues = expandBoxValues(IntStream.range(0, values.length).toArray());
        this.topMargin = values[boxValues[0]];
        this.rightMargin = values[boxValues[1]];
        this.bottomMargin = values[boxValues[2]];
        this.leftMargin = values[boxValues[3]];
        return this;
    }

    /**
     * Sets top margin.
     *
     * @param topMargin top margin
     * @return this style
     */
    public Style marginTop(int topMargin) {
        this.topMargin = topMargin;
        return this;
    }

    /**
     * Sets right margin.
     *
     * @param rightMargin right margin
     * @return this style
     */
    public Style marginRight(int rightMargin) {
        this.rightMargin = rightMargin;
        return this;
    }

    /**
     * Sets bottom margin.
     *
     * @param bottomMargin bottom margin
     * @return this style
     */
    public Style marginBottom(int bottomMargin) {
        this.bottomMargin = bottomMargin;
        return this;
    }

    /**
     * Sets left margin.
     *
     * @param leftMargin left margin
     * @return this style
     */
    public Style marginLeft(int leftMargin) {
        this.leftMargin = leftMargin;
        return this;
    }

    /**
     * Sets the margin background color.
     *
     * @param marginBackgroundColor margin background color
     * @return this style
     */
    public Style marginBackgroundColor(TerminalColor marginBackgroundColor) {
        this.marginBackgroundColor = marginBackgroundColor;
        return this;
    }

    /**
     * Returns the top margin.
     *
     * @return top margin
     */
    public int topMargin() {
        return topMargin;
    }

    /**
     * Configures the border style and which sides to render.
     *
     * @param border border style
     * @param sides which sides to enable (top, right, bottom, left)
     * @return this style
     */
    public Style border(Border border, boolean... sides) {
        if (sides.length == 0) {
            return border(border, true);
        }

        borderDecoration(border);

        int[] boxValues = expandBoxValues(
                IntStream.range(0, sides.length)
                        .map(i -> Boolean.compare(sides[i], true))
                        .toArray()
        );

        return borderTop(sides[boxValues[0]])
                .borderRight(sides[boxValues[1]])
                .borderBottom(sides[boxValues[2]])
                .borderLeft(sides[boxValues[3]]);
    }

    /**
     * Sets the border decoration style.
     *
     * @param borderDecoration border style
     * @return this style
     */
    public Style borderDecoration(Border borderDecoration) {
        this.borderDecoration = borderDecoration;
        return this;
    }

    /**
     * Enables or disables the top border.
     *
     * @param borderTop border flag
     * @return this style
     */
    public Style borderTop(boolean borderTop) {
        this.borderTop = borderTop;
        this.borderTopSet = true;
        return this;
    }

    /**
     * Enables or disables the right border.
     *
     * @param borderRight border flag
     * @return this style
     */
    public Style borderRight(boolean borderRight) {
        this.borderRight = borderRight;
        this.borderRightSet = true;
        return this;
    }

    /**
     * Enables or disables the bottom border.
     *
     * @param borderBottom border flag
     * @return this style
     */
    public Style borderBottom(boolean borderBottom) {
        this.borderBottom = borderBottom;
        this.borderBottomSet = true;
        return this;
    }

    /**
     * Enables or disables the left border.
     *
     * @param borderLeft border flag
     * @return this style
     */
    public Style borderLeft(boolean borderLeft) {
        this.borderLeft = borderLeft;
        this.borderLeftSet = true;
        return this;
    }

    /**
     * Sets border background colors using CSS-like shorthand.
     *
     * @param colors background colors
     * @return this style
     */
    public Style borderBackground(TerminalColor... colors) {
        int[] boxValues = expandBoxValues(
                IntStream.range(0, colors.length).toArray()
        );
        this.borderTopBackground = colors[boxValues[0]];
        this.borderRightBackground = colors[boxValues[1]];
        this.borderBottomBackground = colors[boxValues[2]];
        this.borderLeftBackground = colors[boxValues[3]];

        return this;
    }

    /**
     * Sets the top border background color.
     *
     * @param color background color
     * @return this style
     */
    public Style borderTopBackground(TerminalColor color) {
        this.borderTopBackground = color;
        return this;
    }

    /**
     * Sets the right border background color.
     *
     * @param color background color
     * @return this style
     */
    public Style borderRightBackground(TerminalColor color) {
        this.borderRightBackground = color;
        return this;
    }

    /**
     * Sets the bottom border background color.
     *
     * @param color background color
     * @return this style
     */
    public Style borderBottomBackground(TerminalColor color) {
        this.borderBottomBackground = color;
        return this;
    }

    /**
     * Sets the left border background color.
     *
     * @param color background color
     * @return this style
     */
    public Style borderLeftBackground(TerminalColor color) {
        this.borderLeftBackground = color;
        return this;
    }

    /**
     * Sets border foreground colors using CSS-like shorthand.
     *
     * @param colors foreground colors
     * @return this style
     */
    public Style borderForeground(TerminalColor... colors) {
        int[] boxValues = expandBoxValues(
                IntStream.range(0, colors.length).toArray()
        );
        this.borderTopForeground = colors[boxValues[0]];
        this.borderRightForeground = colors[boxValues[1]];
        this.borderBottomForeground = colors[boxValues[2]];
        this.borderLeftForeground = colors[boxValues[3]];

        return this;
    }

    /**
     * Sets the top border foreground color.
     *
     * @param color foreground color
     * @return this style
     */
    public Style borderTopForeground(TerminalColor color) {
        this.borderTopForeground = color;
        return this;
    }

    /**
     * Sets the right border foreground color.
     *
     * @param color foreground color
     * @return this style
     */
    public Style borderRightForeground(TerminalColor color) {
        this.borderRightForeground = color;
        return this;
    }

    /**
     * Sets the bottom border foreground color.
     *
     * @param color foreground color
     * @return this style
     */
    public Style borderBottomForeground(TerminalColor color) {
        this.borderBottomForeground = color;
        return this;
    }

    /**
     * Sets the left border foreground color.
     *
     * @param color foreground color
     * @return this style
     */
    public Style borderLeftForeground(TerminalColor color) {
        this.borderLeftForeground = color;
        return this;
    }

    /**
     * Sets a transform function applied to content before rendering.
     *
     * @param transformFunction transform function
     * @return this style
     */
    public Style transform(Function<String, String> transformFunction) {
        this.transformFunction = transformFunction;
        return this;
    }

    /**
     * Applies all style rules (colors, margins, padding, borders) to the input strings.
     * Renders the final ANSI sequence.
     *
     * @param strings strings to render
     * @return rendered ANSI string
     */
    public String render(String... strings) {
        AttributedStyle style = new AttributedStyle();
        if (foreground != null) {
            style = foreground.applyAsForeground(style, renderer);
        }
        if (background != null) {
            style = background.applyAsBackground(style, renderer);
        }
        if (bold) {
            style = style.bold();
        }
        if (italic) {
            style = style.italic();
        }
        if (underline) {
            style = style.underline();
        }
        if (faint) {
            style = style.faint();
        }
        if (blink) {
            style = style.blink();
        }
        if (reverse) {
            style = style.inverse();
        }

        List<String> strs = new ArrayList<>(List.of(strings));
        if (value != null && !value.isEmpty()) {
            strs.addFirst(value);
        }
        String string = String.join(" ", strs);

        if (this.transformFunction != null) {
            string = transformFunction.apply(string);
        }

        string = string.replace("\r\n", "\n");

        if (inline) {
            string = string.replace("\n", "");
        }

        if (!inline && width > 0) {
            int wrapAt = width - leftPadding - rightPadding;
            string = new TextWrapper().wrap(string, wrapAt, true);
        }

        // core rendering
        ColorProfile colorProfile = renderer.colorProfile();
        renderer.newStyle();
        string = StyleTextRenderer.render(string, style, colorProfile);

        if (!inline) {
            AttributedStyle st = new AttributedStyle();
            if (background != null) {
                st = background.applyAsBackground(st, renderer);
            }
            string = PaddingDecorator.applyPadding(string, topPadding, rightPadding, bottomPadding, leftPadding, st, renderer);
        }
        if (height > 0) {
            string = AlignmentDecorator.alignTextVertical(string, verticalAlign, height);
        }

        int numLines = (int) string.chars().filter(ch -> ch == '\n').count();
        if (numLines != 0 || width != 0) {
            AttributedStyle st = new AttributedStyle();
            if (background != null) {
                st = background.applyAsBackground(st, renderer);
            }
            string = AlignmentDecorator.alignTextHorizontal(string, horizontalAlign, width, st, colorProfile.colorsCount());
        }
        if (!inline) {
            string = applyBorders(string);

            AttributedStyle st = new AttributedStyle();
            st = marginBackgroundColor.applyAsBackground(st, renderer);

            string = MarginDecorator.applyMargins(string, topMargin, rightMargin, bottomMargin, leftMargin, st, renderer);
        }

        string = StyleSizeLimiter.clamp(string, maxWidth, maxHeight, ellipsis);

        return string;
    }

    private String applyBorders(String string) {
        boolean hasTop = this.borderTop;
        boolean hasRight = this.borderRight;
        boolean hasBottom = this.borderBottom;
        boolean hasLeft = this.borderLeft;

        if (implicitBorders()) {
            hasTop = true;
            hasRight = true;
            hasBottom = true;
            hasLeft = true;
        }

        if (borderDecoration == null || (!hasTop && !hasRight && !hasBottom && !hasLeft)) {
            return string;
        }

        return borderDecoration.applyBorders(string,
                hasTop,
                hasRight,
                hasBottom,
                hasLeft,
                borderTopForeground,
                borderRightForeground,
                borderBottomForeground,
                borderLeftForeground,
                borderTopBackground,
                borderRightBackground,
                borderBottomBackground,
                borderLeftBackground,
                renderer);
    }

    private boolean implicitBorders() {
        return borderDecoration != null && !(borderTopSet || borderRightSet || borderBottomSet || borderLeftSet);
    }

    /**
     * Expands CSS-like shorthand values to a four-element array.
     *
     * @param values input values (1-4)
     * @return expanded array (top, right, bottom, left indices)
     */
    public static int[] expandBoxValues(int... values) {
        int[] result = new int[4];  // top, right, bottom, left

        switch (values.length) {
            case 1:
                Arrays.fill(result, values[0]);
                break;
            case 2:
                result[0] = 0;  // top
                result[1] = 1;  // right
                result[2] = 0;  // bottom
                result[3] = 1;  // left
                break;
            case 3:
                result[0] = 0;  // top
                result[1] = 1;  // right
                result[2] = 2;  // bottom
                result[3] = 1;  // left
                break;
            case 4:
                result[0] = 0;  // top
                result[1] = 1;  // right
                result[2] = 2;  // bottom
                result[3] = 3;
                break;
            default:
                throw new IllegalArgumentException("Expected 1-4 values, got " + values.length);
        }
        return result;
    }

    /**
     * Returns the total frame size (margins + padding + borders).
     *
     * @return frame dimensions
     */
    public Dimensions frameSize() {
        return new Dimensions(getHorizontalFrameSize(), getVerticalFrameSize());
    }

    /**
     * Returns the vertical frame size (margins + padding + borders).
     *
     * @return vertical frame size
     */
    public int getVerticalFrameSize() {
        return getVerticalMargins() + getVerticalPadding() + getVerticalBorderSize();
    }

    /**
     * Returns the combined top and bottom margins.
     *
     * @return vertical margins
     */
    public int getVerticalMargins() {
        return topMargin + bottomMargin;
    }

    /**
     * Returns the combined top and bottom padding.
     *
     * @return vertical padding
     */
    public int getVerticalPadding() {
        return topPadding + bottomPadding;
    }

    /**
     * Returns the combined top and bottom border size.
     *
     * @return vertical border size
     */
    public int getVerticalBorderSize() {
        return getBorderTopSize() + getBorderBottomSize();
    }

    /**
     * Returns the top border size.
     *
     * @return top border size
     */
    public int getBorderTopSize() {
        if (!borderTop && !implicitBorders()) {
            return 0;
        }
        return borderDecoration.getTopSize();
    }

    /**
     * Returns the bottom border size.
     *
     * @return bottom border size
     */
    public int getBorderBottomSize() {
        if (!borderBottom && !implicitBorders()) {
            return 0;
        }
        return borderDecoration.getBottomSize();
    }

    /**
     * Returns the horizontal frame size (margins + padding + borders).
     *
     * @return horizontal frame size
     */
    public int getHorizontalFrameSize() {
        return getHorizontalMargins() + getHorizontalPadding() + getHorizontalBorderSize();
    }

    /**
     * Returns the combined left and right margins.
     *
     * @return horizontal margins
     */
    public int getHorizontalMargins() {
        return rightMargin + leftMargin;
    }

    /**
     * Returns the combined left and right padding.
     *
     * @return horizontal padding
     */
    public int getHorizontalPadding() {
        return rightPadding + leftPadding;
    }

    /**
     * Returns the combined left and right border size.
     *
     * @return horizontal border size
     */
    public int getHorizontalBorderSize() {
        return getBorderLeftSize() + getBorderRightSize();
    }

    /**
     * Returns the left border size.
     *
     * @return left border size
     */
    public int getBorderLeftSize() {
        if (!borderLeft && !implicitBorders()) {
            return 0;
        }
        return borderDecoration.getLeftSize();
    }

    /**
     * Returns the right border size.
     *
     * @return right border size
     */
    public int getBorderRightSize() {
        if (!borderRight && !implicitBorders()) {
            return 0;
        }
        return borderDecoration.getRightSize();
    }

    /**
     * Creates a copy of this style.
     *
     * @return copied style
     */
    public Style copy() {
        return new Style(this);
    }

    /**
     * Inherits unset properties from another style.
     *
     * @param style style to inherit from
     * @return this style
     */
    public Style inherit(Style style) {
        if (this.value == null) this.value = style.value;
        if (this.transformFunction == null) this.transformFunction = style.transformFunction;
        if (this.background instanceof NoColor) this.background = style.background;
        if (this.foreground instanceof NoColor) this.foreground = style.foreground;
        if (!this.bold) this.bold = style.bold;
        if (!this.italic) this.italic = style.italic;
        if (!this.underline) this.underline = style.underline;
        if (!this.blink) this.blink = style.blink;
        if (!this.faint) this.faint = style.faint;
        if (!this.reverse) this.reverse = style.reverse;
        if (!this.inline) this.inline = style.inline;
        if (this.width == 0) this.width = style.width;
        if (this.height == 0) this.height = style.height;
        if (this.maxWidth == 0) this.maxWidth = style.maxWidth;
        if (this.maxHeight == 0) this.maxHeight = style.maxHeight;
        if (this.ellipsis.isEmpty()) this.ellipsis = style.ellipsis;
        if (this.horizontalAlign == Position.Left) this.horizontalAlign = style.horizontalAlign;
        if (this.verticalAlign == Position.Top) this.verticalAlign = style.verticalAlign;

        if (this.borderDecoration == null) this.borderDecoration = style.borderDecoration;

        if (!this.borderTopSet) {
            this.borderTop = style.borderTop;
            this.borderTopSet = style.borderTopSet;
            this.borderTopForeground = style.borderTopForeground;
            this.borderTopBackground = style.borderTopBackground;
        }
        if (!this.borderRightSet) {
            this.borderRight = style.borderRight;
            this.borderRightSet = style.borderRightSet;
            this.borderRightForeground = style.borderRightForeground;
            this.borderRightBackground = style.borderRightBackground;
        }
        if (!this.borderBottomSet) {
            this.borderBottom = style.borderBottom;
            this.borderBottomSet = style.borderBottomSet;
            this.borderBottomForeground = style.borderBottomForeground;
            this.borderBottomBackground = style.borderBottomBackground;
        }
        if (!this.borderLeftSet) {
            this.borderLeft = style.borderLeft;
            this.borderLeftSet = style.borderLeftSet;
            this.borderLeftForeground = style.borderLeftForeground;
            this.borderLeftBackground = style.borderLeftBackground;
        }
        return this;
    }
}
