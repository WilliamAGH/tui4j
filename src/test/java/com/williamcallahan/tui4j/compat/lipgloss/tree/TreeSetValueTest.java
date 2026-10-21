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
 * Upstream: github.com/charmbracelet/lipgloss/tree/tree.go, Lip Gloss v1.0.0.
 * Java {@code setValue} delegates to {@code root}: tree values adopt their root value and
 * children, while other nodes use their string representation, matching upstream Root.
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

    /**
     * Preserves the non-tree node's string representation, as upstream Root does.
     */
    @Test
    void testSetValueWithANodeUsesItsStringRepresentation() {
        Node node = new Leaf("Stored value") {
            /**
             * Supplies the node's display text independently of its stored value.
             */
            @Override
            public String toString() {
                return "Rendered node";
            }
        };

        assertThat(new Tree().setValue(node).render()).isEqualTo("Rendered node");
    }
}
