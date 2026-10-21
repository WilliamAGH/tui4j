package com.williamcallahan.tui4j.compat.bubbles.viewport;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.lipgloss.Style;

/**
 * Scrollable viewport for viewing content larger than the screen.
 * <p>
 * Port of {@code bubbles/viewport/viewport.go}.
 * Handles vertical scrolling and panning of wrapped or raw text content.
 *
 * @see <a href="https://github.com/charmbracelet/bubbles/blob/main/viewport/viewport.go">bubbles/viewport/viewport.go</a>
 */
public class Viewport implements Model {

    private int width;
    private int height;
    private KeyMap keyMap;
    private boolean mouseWheelEnabled;
    private int mouseWheelDelta;
    private int yOffset;
    private int xOffset;
    private int horizontalStep;
    private int yPosition;
    private Style style;
    private boolean highPerformanceRendering;

    private final ViewportContent content;

    /** Applies defaults during construction so updates preserve caller configuration. */
    public Viewport() {
        this.width = 0;
        this.height = 0;
        this.keyMap = new KeyMap();
        this.mouseWheelEnabled = true;
        this.mouseWheelDelta = 3;
        this.yOffset = 0;
        this.xOffset = 0;
        this.horizontalStep = 0;
        this.yPosition = 0;
        this.style = Style.newStyle();
        this.highPerformanceRendering = false;
        this.content = new ViewportContent();
    }

    /** Creates Viewport with initial content.
     * @param content initial text content
     */
    public Viewport(String content) {
        this();
        setContent(content);
    }

    /**
     * Creates Viewport with content and height.
     *
     * @param content initial text content
     * @param height viewport height
     */
    public Viewport(String content, int height) {
        this(content);
        setHeight(height);
    }

    /**
     * Creates Viewport with content and dimensions.
     *
     * @param content initial text content
     * @param width viewport width
     * @param height viewport height
     */
    public Viewport(String content, int width, int height) {
        this(content, height);
        setWidth(width);
    }

    /**
     * Creates Viewport with content, dimensions, and key bindings.
     *
     * @param content initial text content
     * @param width viewport width
     * @param height viewport height
     * @param keyMap custom key bindings
     */
    public Viewport(String content, int width, int height, KeyMap keyMap) {
        this(content, width, height);
        this.keyMap = keyMap;
    }

    /**
     * Creates an empty viewport with the requested dimensions.
     *
     * @param width width
     * @param height height
     * @return result
     */
    public static Viewport create(int width, int height) {
        Viewport viewport = new Viewport();
        viewport.setWidth(width);
        viewport.setHeight(height);
        return viewport;
    }

    /** Supplies the initial command for the model.
     * @return null because the viewport needs no initial command
     */
    @Override
    public Command init() {
        return null;
    }

    /** Reports whether vertical scrolling has reached the first row.
     * @return whether the vertical offset is at or above the first row
     */
    public boolean atTop() {
        return yOffset <= 0;
    }

    /** Reports whether vertical scrolling has reached its content bound.
     * @return whether the vertical offset has reached the last page
     */
    public boolean atBottom() {
        return yOffset >= maxYOffset();
    }

    /** Detects an offset beyond the current content bound after resizing.
     * @return whether the vertical offset exceeds the last page
     */
    public boolean pastBottom() {
        return yOffset > maxYOffset();
    }

    /** Reports vertical progress, treating content that fits as fully visible.
     * @return vertical scroll progress between zero and one
     */
    public double scrollPercent() {
        if (content.lineCount() == 0 || height >= content.lineCount()) {
            return 1.0;
        }
        double y = (double) yOffset;
        double h = (double) height;
        double t = (double) content.lineCount();
        double v = y / (t - h);
        return max(0.0, min(1.0, v));
    }

    /** Reports horizontal progress using the measured cell width.
     * @return horizontal scroll progress between zero and one
     */
    public double horizontalScrollPercent() {
        int effectiveWidth = max(0, width - style.getHorizontalFrameSize());
        if (content.lineCount() == 0 || effectiveWidth >= content.longestLineWidth()) {
            return 1.0;
        }
        double x = (double) xOffset;
        double w = (double) effectiveWidth;
        double t = (double) content.longestLineWidth();
        double v = x / (t - w);
        return max(0.0, min(1.0, v));
    }

    /**
     * Replaces the viewport content and recalculates dimensions.
     * Resets position if content shrinks above current scroll.
     *
     * @param content new content
     */
    public void setContent(String content) {
        this.content.setContent(content);
        setXOffset(this.xOffset);

        if (yOffset > this.content.lineCount() - 1) {
            gotoBottom();
        }
    }

    /** Includes the style frame when determining the vertical content bound.
     * @return the maximum vertical offset including the style frame
     */
    private int maxYOffset() {
        return max(0, content.lineCount() - height + style.getVerticalFrameSize());
    }

    /** Advances by one viewport height unless already at the bottom. */
    public void pageDown() {
        if (!atBottom()) {
            scrollDown(height);
        }
    }

    /** Moves back one viewport height unless already at the top. */
    public void pageUp() {
        if (!atTop()) {
            scrollUp(height);
        }
    }

    /**
     * Advances by half the viewport height unless already at the bottom.
     */
    public void halfPageDown() {
        if (!atBottom()) {
            scrollDown(height / 2);
        }
    }

    /**
     * Moves back half the viewport height unless already at the top.
     */
    public void halfPageUp() {
        if (!atTop()) {
            scrollUp(height / 2);
        }
    }

    /** Advances by the requested rows within the current content bounds.
     * @param n number of rows to scroll down
     */
    public void scrollDown(int n) {
        if (atBottom() || n == 0 || content.lineCount() == 0) {
            return;
        }
        setYOffsetInternal(yOffset + n);
    }

    /** Moves back the requested rows within the current content bounds.
     * @param n number of rows to scroll up
     */
    public void scrollUp(int n) {
        if (atTop() || n == 0 || content.lineCount() == 0) {
            return;
        }
        setYOffsetInternal(yOffset - n);
    }

    /** Sets a nonnegative step for horizontal key navigation.
     * @param n horizontal navigation step in terminal cells, clamped to zero
     */
    public void setHorizontalStep(int n) {
        this.horizontalStep = max(n, 0);
    }

    /** Returns the horizontal step that key and wheel panning use.
     * @return horizontal navigation step in terminal cells
     */
    int horizontalStep() {
        return horizontalStep;
    }

    /** Constrains the horizontal offset to the measured content width.
     * @param n requested horizontal offset in terminal cells
     */
    public void setXOffset(int n) {
        int effectiveWidth = max(0, width - style.getHorizontalFrameSize());
        this.xOffset = clamp(n, 0, max(0, content.longestLineWidth() - effectiveWidth));
    }

    /** Moves left while respecting the horizontal content bounds.
     * @param n number of terminal cells to scroll left
     */
    public void scrollLeft(int n) {
        setXOffset(xOffset - n);
    }

    /** Moves right while respecting the horizontal content bounds.
     * @param n number of terminal cells to scroll right
     */
    public void scrollRight(int n) {
        setXOffset(xOffset + n);
    }

    /** Returns the content length used to determine scrollable rows.
     * @return total number of content lines
     */
    public int totalLineCount() {
        return content.lineCount();
    }

    /** Counts the rows selected for the current viewport and style frame.
     * @return number of visible content lines
     */
    public int visibleLineCount() {
        return content.visibleLines(width, height, style, xOffset, yOffset).size();
    }

    /**
     * Returns to the first row unless already at the top.
     */
    public void gotoTop() {
        if (!atTop()) {
            setYOffsetInternal(0);
        }
    }

    /**
     * Moves to the last page allowed by the current content and style frame.
     */
    public void gotoBottom() {
        setYOffsetInternal(maxYOffset());
    }

    /**
     * Applies an incoming message and returns the next model state.
     *
     * @param msg msg
     * @return next model state and command
     */
    @Override
    public UpdateResult<Viewport> update(Message msg) {
        ViewportInputHandler.apply(this, msg);
        return UpdateResult.from(this);
    }

    /** Renders the model view for display.
     * @return rendered viewport content or reserved rows for high performance rendering
     */
    @Override
    public String view() {
        if (highPerformanceRendering) {
            return "\n".repeat(max(0, height - 1));
        }

        return content.view(width, height, style, xOffset, yOffset);
    }

    /**
     * Constrains a value even when the supplied bounds are reversed.
     *
     * @param v v
     * @param low low
     * @param high high
     * @return result
     */
    private int clamp(int v, int low, int high) {
        if (high < low) {
            int temp = low;
            low = high;
            high = temp;
        }
        return max(low, min(high, v));
    }

    /** Constrains vertical movement to the current content and style frame.
     * @param n requested vertical offset in content rows
     */
    private void setYOffsetInternal(int n) {
        this.yOffset = clamp(n, 0, maxYOffset());
    }

    /** Updates the width.
     * @param width viewport width in terminal cells
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /** Updates the height.
     * @param height viewport height in rows
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /** Returns the width.
     * @return viewport width in terminal cells
     */
    public int getWidth() {
        return width;
    }

    /** Returns the height.
     * @return viewport height in rows
     */
    public int getHeight() {
        return height;
    }

    /** Returns the key map.
     * @return configured navigation key bindings
     */
    public KeyMap getKeyMap() {
        return keyMap;
    }

    /** Reports whether the caller has enabled mouse wheel scrolling.
     * @return whether mouse wheel scrolling is enabled
     */
    public boolean isMouseWheelEnabled() {
        return mouseWheelEnabled;
    }

    /** Stores whether the caller enables mouse wheel scrolling.
     * @param mouseWheelEnabled whether to enable mouse wheel scrolling
     */
    public void setMouseWheelEnabled(boolean mouseWheelEnabled) {
        this.mouseWheelEnabled = mouseWheelEnabled;
    }

    /** Returns the configured number of rows per mouse wheel step.
     * @return configured number of rows per mouse wheel step
     */
    public int getMouseWheelDelta() {
        return mouseWheelDelta;
    }

    /** Stores the requested number of rows per mouse wheel step.
     * @param mouseWheelDelta number of rows per mouse wheel step
     */
    public void setMouseWheelDelta(int mouseWheelDelta) {
        this.mouseWheelDelta = mouseWheelDelta;
    }

    /** Returns the first visible content row.
     * @return vertical offset in content rows
     */
    public int getYOffset() {
        return yOffset;
    }

    /** Returns the style.
     * @return viewport style
     */
    public Style getStyle() {
        return style;
    }

    /** Updates the style.
     * @param style viewport style
     */
    public void setStyle(Style style) {
        this.style = style;
    }

    /** Returns whether high performance rendering is enabled.
     * @return whether high performance rendering is enabled
     */
    public boolean isHighPerformanceRendering() {
        return highPerformanceRendering;
    }

    /** Enables or disables high performance rendering.
     * @param highPerformanceRendering whether to enable high performance rendering
     */
    public void setHighPerformanceRendering(boolean highPerformanceRendering) {
        this.highPerformanceRendering = highPerformanceRendering;
    }

    /** Returns the screen row used for high-performance rendering.
     * @return screen row used for high-performance rendering
     */
    public int getYPosition() {
        return yPosition;
    }

    /** Sets the screen row used for high-performance rendering.
     * @param yPosition screen row used for high-performance rendering
     */
    public void setYPosition(int yPosition) {
        this.yPosition = yPosition;
    }

    /** Returns the horizontal offset measured in terminal cells.
     * @return horizontal offset in terminal cells
     */
    public int getXOffset() {
        return xOffset;
    }

    /** Returns the last vertical offset allowed by content and style frame.
     * @return maximum vertical offset including the style frame
     */
    public int getMaxYOffset() {
        return maxYOffset();
    }
}
