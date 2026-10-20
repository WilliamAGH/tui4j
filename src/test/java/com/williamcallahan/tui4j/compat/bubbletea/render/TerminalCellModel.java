package com.williamcallahan.tui4j.compat.bubbletea.render;

import com.williamcallahan.tui4j.compat.x.ansi.StringWidth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Applies renderer byte sequences to visible terminal cells for renderer tests.
 */
final class TerminalCellModel {

    private int width;
    private int height;
    private char[][] cells;
    private boolean[] touchedRightMargin;
    private int row;
    private int column;
    private boolean wrapPending;
    private boolean autoWrap = true;

    /**
     * Creates a blank terminal screen.
     *
     * @param width terminal columns
     * @param height terminal rows
     */
    TerminalCellModel(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = blankCells(width, height);
        this.touchedRightMargin = new boolean[height];
    }

    /**
     * Applies printable text and the cursor/erase CSI operations emitted by the renderer.
     *
     * @param value renderer output
     */
    void apply(String value) {
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == '\u001b' && offset + 1 < value.length()
                && value.charAt(offset + 1) == '[') {
                offset = applyCsi(value, offset + 2);
            } else if (codePoint == '\r') {
                column = 0;
                wrapPending = false;
                offset++;
            } else if (codePoint == '\n') {
                lineFeed();
                offset++;
            } else if (codePoint >= 0x20) {
                writeCodePoint(codePoint);
                offset += Character.charCount(codePoint);
            } else {
                offset += Character.charCount(codePoint);
            }
        }
    }

    /**
     * Reflows rows that reached the old right margin, matching common terminal resize behavior.
     *
     * @param newWidth new terminal columns
     * @param newHeight new terminal rows
     */
    void resize(int newWidth, int newHeight) {
        List<String> reflowed = new ArrayList<>();
        int newCursorRow = 0;
        int lastUsedRow = row;
        for (int sourceRow = height - 1; sourceRow > row; sourceRow--) {
            if (touchedRightMargin[sourceRow] || lastContentColumn(cells[sourceRow]) > 0) {
                lastUsedRow = sourceRow;
                break;
            }
        }
        for (int sourceRow = 0; sourceRow <= lastUsedRow; sourceRow++) {
            int contentWidth = touchedRightMargin[sourceRow]
                ? width
                : lastContentColumn(cells[sourceRow]);
            String content = new String(cells[sourceRow], 0, contentWidth);
            int rowCount = Math.max(1, (contentWidth + newWidth - 1) / newWidth);
            for (int part = 0; part < rowCount; part++) {
                int start = part * newWidth;
                int end = Math.min(content.length(), start + newWidth);
                reflowed.add(content.substring(start, end));
            }
            if (sourceRow < row) {
                newCursorRow += rowCount;
            } else if (sourceRow == row) {
                newCursorRow += Math.min(column / newWidth, rowCount - 1);
            }
        }

        int clippedRows = Math.max(0, reflowed.size() - newHeight);
        this.width = newWidth;
        this.height = newHeight;
        this.cells = blankCells(newWidth, newHeight);
        this.touchedRightMargin = new boolean[newHeight];
        for (int targetRow = 0; targetRow < Math.min(newHeight, reflowed.size()); targetRow++) {
            String content = reflowed.get(targetRow + clippedRows);
            content.getChars(0, content.length(), cells[targetRow], 0);
            touchedRightMargin[targetRow] = content.length() == newWidth;
        }
        this.row = Math.clamp(newCursorRow - clippedRows, 0, newHeight - 1);
        this.column = Math.clamp(column % newWidth, 0, newWidth - 1);
        this.wrapPending = false;
    }

    /**
     * Returns a visible row including trailing blank cells.
     *
     * @param index zero-based row
     * @return visible cells
     */
    String line(int index) {
        return new String(cells[index]);
    }

    /**
     * Returns the cursor row.
     *
     * @return zero-based row
     */
    int cursorRow() {
        return row;
    }

    /**
     * Returns the cursor column.
     *
     * @return zero-based column
     */
    int cursorColumn() {
        return column;
    }

    /**
     * Places the cursor for modeling entry into an inline render region.
     *
     * @param row zero-based row
     * @param column zero-based column
     */
    void positionCursor(int row, int column) {
        this.row = Math.clamp(row, 0, height - 1);
        this.column = Math.clamp(column, 0, width - 1);
        this.wrapPending = false;
    }

    /**
     * Clears the visible screen while retaining its dimensions.
     */
    void clear() {
        cells = blankCells(width, height);
        touchedRightMargin = new boolean[height];
        row = 0;
        column = 0;
        wrapPending = false;
    }

    /**
     * Parses and applies one CSI operation.
     *
     * @param value complete renderer output
     * @param parameterStart first parameter character
     * @return offset immediately after the CSI operation
     */
    private int applyCsi(String value, int parameterStart) {
        int finalOffset = parameterStart;
        while (finalOffset < value.length()) {
            char character = value.charAt(finalOffset);
            if (character >= '@' && character <= '~') break;
            finalOffset++;
        }
        if (finalOffset == value.length()) return finalOffset;

        char operation = value.charAt(finalOffset);
        String parameters = value.substring(parameterStart, finalOffset);
        switch (operation) {
            case 'A' -> moveVertically(-parameter(parameters, 1));
            case 'B' -> moveVertically(parameter(parameters, 1));
            case 'H', 'f' -> positionCursor(parameters);
            case 'J' -> eraseScreenBelow();
            case 'K' -> eraseLineRight();
            case 'h' -> setPrivateMode(parameters, true);
            case 'l' -> setPrivateMode(parameters, false);
            default -> {
                // SGR and unsupported private modes do not alter modeled cells or cursor position.
            }
        }
        return finalOffset + 1;
    }

    /**
     * Parses the first positive CSI parameter.
     *
     * @param parameters CSI parameter text
     * @param defaultValue value used for an omitted parameter
     * @return parsed parameter
     */
    private static int parameter(String parameters, int defaultValue) {
        if (parameters.isEmpty()) return defaultValue;
        int separator = parameters.indexOf(';');
        String first = separator < 0 ? parameters : parameters.substring(0, separator);
        return first.isEmpty() ? defaultValue : Integer.parseInt(first);
    }

    /**
     * Applies a DEC private mode used by the renderer.
     *
     * @param parameters CSI private-mode parameters
     * @param enabled whether the mode is being set or reset
     */
    private void setPrivateMode(String parameters, boolean enabled) {
        if ("?7".equals(parameters)) autoWrap = enabled;
    }

    /**
     * Applies a one-based ANSI cursor position.
     *
     * @param parameters row and column parameters
     */
    private void positionCursor(String parameters) {
        String[] coordinates = parameters.split(";", -1);
        int requestedRow = coordinates.length == 0 || coordinates[0].isEmpty()
            ? 1 : Integer.parseInt(coordinates[0]);
        int requestedColumn = coordinates.length < 2 || coordinates[1].isEmpty()
            ? 1 : Integer.parseInt(coordinates[1]);
        row = Math.clamp(requestedRow - 1, 0, height - 1);
        column = Math.clamp(requestedColumn - 1, 0, width - 1);
        wrapPending = false;
    }

    /**
     * Moves the cursor without scrolling.
     *
     * @param delta signed row count
     */
    private void moveVertically(int delta) {
        row = Math.clamp(row + delta, 0, height - 1);
        wrapPending = false;
    }

    /**
     * Moves down one row, scrolling at the bottom edge.
     */
    private void lineFeed() {
        wrapPending = false;
        if (row == height - 1) {
            scrollUp();
        } else {
            row++;
        }
    }

    /**
     * Writes one printable code point using delayed right-margin wrapping.
     *
     * @param codePoint Unicode code point
     */
    private void writeCodePoint(int codePoint) {
        if (wrapPending) {
            if (autoWrap) {
                column = 0;
                lineFeed();
            } else {
                wrapPending = false;
            }
        }
        int displayWidth = StringWidth.stringWidth(new String(Character.toChars(codePoint)));
        if (displayWidth == 0) return;
        cells[row][column] = Character.isBmpCodePoint(codePoint) ? (char) codePoint : '\ufffd';
        for (int occupied = 1; occupied < displayWidth && column + occupied < width; occupied++) {
            cells[row][column + occupied] = ' ';
        }
        if (column + displayWidth >= width) {
            column = width - 1;
            wrapPending = autoWrap;
            touchedRightMargin[row] = true;
        } else {
            column += displayWidth;
        }
    }

    /**
     * Erases cells from the cursor through the end of its row.
     */
    private void eraseLineRight() {
        Arrays.fill(cells[row], column, width, ' ');
        touchedRightMargin[row] = false;
        wrapPending = false;
    }

    /**
     * Erases cells from the cursor through the end of the screen.
     */
    private void eraseScreenBelow() {
        eraseLineRight();
        for (int index = row + 1; index < height; index++) {
            Arrays.fill(cells[index], ' ');
            touchedRightMargin[index] = false;
        }
    }

    /**
     * Scrolls visible cells upward by one row.
     */
    private void scrollUp() {
        for (int index = 1; index < height; index++) {
            cells[index - 1] = cells[index];
            touchedRightMargin[index - 1] = touchedRightMargin[index];
        }
        cells[height - 1] = blankRow(width);
        touchedRightMargin[height - 1] = false;
    }

    /**
     * Finds the end of non-blank row content.
     *
     * @param content row cells
     * @return exclusive content end
     */
    private static int lastContentColumn(char[] content) {
        for (int index = content.length - 1; index >= 0; index--) {
            if (content[index] != ' ') return index + 1;
        }
        return 0;
    }

    /**
     * Allocates a blank screen.
     *
     * @param width terminal columns
     * @param height terminal rows
     * @return blank cells
     */
    private static char[][] blankCells(int width, int height) {
        char[][] result = new char[height][];
        for (int index = 0; index < height; index++) result[index] = blankRow(width);
        return result;
    }

    /**
     * Allocates one blank row.
     *
     * @param width terminal columns
     * @return blank row
     */
    private static char[] blankRow(int width) {
        char[] result = new char[width];
        Arrays.fill(result, ' ');
        return result;
    }
}
