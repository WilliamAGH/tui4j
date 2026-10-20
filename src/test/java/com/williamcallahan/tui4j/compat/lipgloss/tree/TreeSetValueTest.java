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
 * Tests {@link Tree#setValue(Object)} against the upstream contract it ports.
 * <p>
 * Upstream: github.com/charmbracelet/lipgloss/tree/tree.go ({@code Tree.SetValue} delegates to
 * {@code Tree.Root}, so setting a node value adopts the subtree's root value and children).
 * Verified against Go 1.26 with lipgloss v1.1.0:
 * {@code t := tree.New(); t.SetValue(tree.Root("I am groot").Child("leaves"))} yields
 * {@code Value() == "I am groot"}, {@code Children().Length() == 1}, and
 * {@code String() == "I am groot\n└── leaves"}; {@code SetValue} of a leaf or a plain string
 * yields that value unchanged.
 */
class TreeSetValueTest {

    @BeforeEach
    void setUp() {
        // Own the terminal capability source: rendering must not depend on another test class
        // having installed the shared provider first.
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    void testSetValueWithSubtreeUsesRootSemantics() {
        // given
        Tree tree = new Tree().root("Old");

        // when
        tree.setValue(Tree.withRoot("I am groot").child("leaves"));

        // then
        assertThat(tree.value()).isEqualTo("I am groot");
        assertThat(tree.children().length()).isEqualTo(1);
        assertThat(tree.render()).isEqualTo("""
                I am groot
                └── leaves""");
    }

    @Test
    void testSetValueWithAStringKeepsTheRootValue() {
        // given
        Tree tree = new Tree().root("Old").child("Foo");

        // when
        tree.setValue("Plain");

        // then
        assertThat(tree.value()).isEqualTo("Plain");
        assertThat(tree.render()).isEqualTo("""
                Plain
                └── Foo""");
    }

    @Test
    void testSetValueWithALeafUsesItsValue() {
        // given
        Tree tree = new Tree().root("Old");

        // when
        tree.setValue(Tree.newLeaf("Leafy", false));

        // then
        assertThat(tree.value()).isEqualTo("Leafy");
        assertThat(tree.render()).isEqualTo("Leafy");
    }
}
