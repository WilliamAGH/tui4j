package com.williamcallahan.tui4j.compat.lipgloss;

/**
 * Port of Lip Gloss position values.
 * Upstream: lipgloss/position.go
 */
public final class Position {
    /** Top-aligned position. */
    public static final Position Top = new Position(0.0);
    /** Bottom-aligned position. */
    public static final Position Bottom = new Position(1.0);
    /** Center position. */
    public static final Position Center = new Position(0.5);
    /** Left-aligned position. */
    public static final Position Left = new Position(0.0);
    /** Right-aligned position. */
    public static final Position Right = new Position(1.0);

    private final double value;

    /**
     * Creates a position value.
     *
     * @param value normalized position value
     */
    public Position(double value) {
        this.value = value;
    }

    /**
     * Returns the normalized position value.
     *
     * @return normalized value between 0 and 1
     */
    public double value() {
        return Math.min(1, Math.max(0, this.value));
    }

    /**
     * Compares positions by value, matching upstream where {@code Position} is a
     * float and the constants alias by value ({@code Top == Left == 0} and
     * {@code Bottom == Right == 1}).
     * <p>
     * Identity comparison would send an aliased position such as
     * {@code Position.Right} used on the vertical axis to the middle branch of
     * {@link Renderer#placeVertical} and
     * {@link com.williamcallahan.tui4j.compat.lipgloss.align.AlignmentDecorator#alignTextVertical},
     * which diverges from upstream (it aligns or places at the opposite end).
     *
     * @param other object to compare
     * @return true when the other object is a position with the same value
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Position position)) {
            return false;
        }
        return value == position.value;
    }

    /**
     * Returns a hash consistent with {@link #equals(Object)}.
     *
     * @return hash of the position value
     */
    @Override
    public int hashCode() {
        // Adding zero normalizes -0.0 to 0.0, which equals treats as the same value.
        return Double.hashCode(value + 0.0);
    }
}
