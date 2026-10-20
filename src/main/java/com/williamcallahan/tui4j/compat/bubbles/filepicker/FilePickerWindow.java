package com.williamcallahan.tui4j.compat.bubbles.filepicker;

/**
 * Cursor and visible-window state of the file picker.
 * <p>
 * Port of the {@code selected}/{@code min}/{@code max} fields and their
 * stack history in
 * github.com/charmbracelet/bubbles/filepicker/filepicker.go, including the
 * height bookkeeping from {@code SetHeight} and the {@code tea.WindowSizeMsg}
 * branch of {@code Update}.
 */
final class FilePickerWindow {

    private static final int MARGIN_BOTTOM = 5;

    private int height;
    private int selected;
    private int min;
    private int max;
    private final IntStack selectedStack = new IntStack();
    private final IntStack minStack = new IntStack();
    private final IntStack maxStack = new IntStack();

    /**
     * Applies an explicit picker height, shrinking the window when it no
     * longer fits (upstream {@code SetHeight}).
     *
     * @param height height in rows
     */
    void setHeight(int height) {
        this.height = Math.max(0, height);
        if (max > this.height - 1) {
            max = Math.max(0, Math.max(min, min + this.height - 1));
        }
    }

    /**
     * Applies a terminal resize, deriving height from the terminal when
     * auto-height is on and clamping the window to the file count.
     *
     * @param fileCount  number of listed entries
     * @param termHeight terminal height in rows
     * @param autoHeight whether the picker derives its height
     */
    void setTerminalSize(int fileCount, int termHeight, boolean autoHeight) {
        if (autoHeight) {
            height = Math.max(0, termHeight - MARGIN_BOTTOM);
        }
        clamp(fileCount);
    }

    /**
     * Clamps selection and window bounds after the listing changes so a
     * shrinking directory cannot leave indices out of bounds.
     *
     * @param fileCount number of listed entries
     */
    void clamp(int fileCount) {
        int lastIndex = Math.max(0, fileCount - 1);
        selected = Math.min(selected, lastIndex);
        min = Math.min(min, selected);
        max = Math.min(lastIndex, Math.max(0, min + height - 1));
    }

    /**
     * Computes the max index clamped to non-negative values.
     * Prevents -1 when height is zero.
     *
     * @return non-negative max index
     */
    int computeMaxIndex() {
        return Math.max(0, height - 1);
    }

    /**
     * Returns the current picker height.
     *
     * @return height in rows
     */
    int height() {
        return height;
    }

    /**
     * Returns the selected row index.
     *
     * @return selected index
     */
    int selected() {
        return selected;
    }

    /**
     * Sets the selected row index.
     *
     * @param selected selected index
     */
    void selected(int selected) {
        this.selected = selected;
    }

    /**
     * Returns the first visible row index.
     *
     * @return minimum visible index
     */
    int min() {
        return min;
    }

    /**
     * Sets the first visible row index.
     *
     * @param min minimum visible index
     */
    void min(int min) {
        this.min = min;
    }

    /**
     * Returns the last visible row index.
     *
     * @return maximum visible index
     */
    int max() {
        return max;
    }

    /**
     * Sets the last visible row index.
     *
     * @param max maximum visible index
     */
    void max(int max) {
        this.max = max;
    }

    /**
     * Returns the view-history selection stack.
     *
     * @return selection stack
     */
    IntStack selectedStack() {
        return selectedStack;
    }

    /**
     * Returns the view-history minimum stack.
     *
     * @return minimum stack
     */
    IntStack minStack() {
        return minStack;
    }

    /**
     * Returns the view-history maximum stack.
     *
     * @return maximum stack
     */
    IntStack maxStack() {
        return maxStack;
    }
}
