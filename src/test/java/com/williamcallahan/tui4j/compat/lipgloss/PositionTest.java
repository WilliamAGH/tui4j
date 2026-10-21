package com.williamcallahan.tui4j.compat.lipgloss;

import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.williamcallahan.tui4j.compat.lipgloss.Style.newStyle;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that position aliases behave like upstream Lip Gloss.
 * <p>
 * Upstream: lipgloss/position.go, where {@code Position} is a float and the
 * constants alias by value: {@code Top == Left == 0} and
 * {@code Bottom == Right == 1}. Every placement, alignment, and join switch
 * compares those values, so passing either alias selects the start branch (0)
 * or the end branch (1) - never the middle branch.
 */
class PositionTest {

    @BeforeEach
    void setUp() {
        Renderer.defaultRenderer().setColorProfile(ColorProfile.TrueColor);
    }

    @Test
    void test_ShouldConsiderAliasedPositionsEqual() {
        assertThat(Position.Top).isEqualTo(Position.Left);
        assertThat(Position.Bottom).isEqualTo(Position.Right);
        assertThat(Position.Center).isEqualTo(new Position(0.5));
        assertThat(Position.Top).isNotEqualTo(Position.Center);
        assertThat(Position.Top).isNotEqualTo(Position.Bottom);
        assertThat(Position.Top.hashCode()).isEqualTo(Position.Left.hashCode());
        assertThat(Position.Bottom.hashCode()).isEqualTo(Position.Right.hashCode());
    }

    @Test
    void test_ShouldPlaceAtTheStartForTopAndLeft() {
        // Upstream renders the content first and pads the end for position 0.
        assertThat(Renderer.defaultRenderer().placeHorizontal(6, Position.Top, "ab"))
            .isEqualTo("ab    ");
        assertThat(Renderer.defaultRenderer().placeVertical(3, Position.Left, "ab"))
            .isEqualTo("ab\n  \n  ");
    }

    @Test
    void test_ShouldPlaceAtTheEndForBottomAndRight() {
        // Upstream pads the start and renders the content last for position 1.
        assertThat(Renderer.defaultRenderer().placeHorizontal(6, Position.Bottom, "ab"))
            .isEqualTo("    ab");
        assertThat(Renderer.defaultRenderer().placeVertical(3, Position.Right, "ab"))
            .isEqualTo("  \n  \nab");
    }

    @Test
    void test_ShouldPlaceWithAliasedPositionsOnBothAxes() {
        // Upstream: Place(6, 3, Bottom, Left, "ab") right-aligns and top-aligns.
        assertThat(Renderer.defaultRenderer().place(6, 3, Position.Bottom, Position.Left, "ab"))
            .isEqualTo("    ab\n      \n      ");
    }

    @Test
    void test_ShouldAlignVerticallyWithAliasedPositions() {
        // Upstream: Height(3).Align(Center, Right) pads above the content.
        assertThat(newStyle().height(3).align(Position.Center, Position.Right).render("foo"))
            .isEqualTo("   \n   \nfoo");
        assertThat(newStyle().height(3).align(Position.Center, Position.Left).render("foo"))
            .isEqualTo("foo\n   \n   ");
    }

    @Test
    void test_ShouldAlignHorizontallyWithAliasedPositions() {
        // Upstream: Width(6).Align(Bottom) right-aligns the content.
        assertThat(newStyle().width(6).align(Position.Bottom).render("ab"))
            .isEqualTo("    ab");
        assertThat(newStyle().width(6).align(Position.Top).render("ab"))
            .isEqualTo("ab    ");
    }

    @Test
    void test_ShouldJoinAtTheStartForTopAndLeft() {
        // Join switches treat position 0 as the start branch for both aliases.
        assertThat(Join.joinHorizontal(Position.Left, "A", "B\nB"))
            .isEqualTo("AB\n B");
        assertThat(Join.joinVertical(Position.Top, "A", "BBB"))
            .isEqualTo("A  \nBBB");
    }
}
