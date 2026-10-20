package com.williamcallahan.tui4j.compat.bubbles.table;

import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

/**
 * Port of Bubbles table Styles.
 * Bubble Tea: bubbles/table/table.go
 */
public class Styles {

    private Style header;
    private Style cell;
    private Style selected;

    /**
     * Creates default table styles.
     * <p>
     * Mirrors upstream {@code DefaultStyles}: bold 212 (pink) selected row,
     * bold padded header, padded cells.
     */
    public Styles() {
        this.header = Style.newStyle().bold(true).padding(0, 1);
        this.cell = Style.newStyle().padding(0, 1);
        this.selected = Style.newStyle().bold(true).foreground(Color.color("212"));
    }

    /**
     * Creates default table styles.
     *
     * @return default styles
     */
    public static Styles defaultStyles() {
        return new Styles();
    }

    /**
     * Returns the header style.
     *
     * @return header style
     */
    public Style header() {
        return header;
    }

    /**
     * Sets the header style.
     *
     * @param header style to set
     * @return this styles instance
     */
    public Styles header(Style header) {
        this.header = header;
        return this;
    }

    /**
     * Returns the cell style.
     *
     * @return cell style
     */
    public Style cell() {
        return cell;
    }

    /**
     * Sets the cell style.
     *
     * @param cell style to set
     * @return this styles instance
     */
    public Styles cell(Style cell) {
        this.cell = cell;
        return this;
    }

    /**
     * Returns the selected style.
     *
     * @return selected style
     */
    public Style selected() {
        return selected;
    }

    /**
     * Sets the selected style.
     *
     * @param selected style to set
     * @return this styles instance
     */
    public Styles selected(Style selected) {
        this.selected = selected;
        return this;
    }
}
