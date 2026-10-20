package com.williamcallahan.tui4j.compat.lipgloss.tree;

import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link Tree#toString()} against the upstream contract it ports.
 * <p>
 * Upstream: github.com/charmbracelet/lipgloss/tree/tree.go - the {@code Node} interface embeds
 * {@code fmt.Stringer} and {@code Tree.String()} is
 * {@code t.ensureRenderer().render(t, true, "")} (identical in lipgloss v0.13.0, v1.0.0, and
 * v1.1.0), so stringifying a tree renders it; every upstream example and test prints
 * {@code tr.String()}. In this repository {@code Leaf}, {@code List}, and {@code table.Table}
 * all override {@code toString()} for the same reason.
 */
class TreeStringTest {

    @BeforeEach
    void setUp() {
        // Own the terminal capability source instead of inheriting another class's provider.
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    void testToStringRendersTheTree() {
        // given
        Tree tree = new Tree().root("Root").child("Foo", "Bar");

        // then
        assertThat(tree.toString()).isEqualTo("""
                Root
                ├── Foo
                └── Bar""");
        assertThat(tree.toString()).isEqualTo(tree.render());
    }

    @Test
    void testToStringOfAHiddenTreeIsEmpty() {
        // given - control: stringifying renders, it does not fabricate content
        Tree tree = new Tree().root("Root").child("Foo").hide();

        // then
        assertThat(tree.toString()).isEmpty();
        assertThat(tree.toString()).isEqualTo(tree.render());
    }
}
