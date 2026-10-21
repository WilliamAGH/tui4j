package com.williamcallahan.tui4j.compat.bubbles.viewport;

import static org.assertj.core.api.Assertions.assertThat;

import com.williamcallahan.tui4j.compat.lipgloss.Style;
import org.junit.jupiter.api.Test;

/** Verifies the rendered viewport occupies exactly its height, as upstream {@code View} does. */
class ViewportViewHeightTest {

    @Test
    void contentTallerThanTheViewportRendersExactlyItsHeight() {
        Viewport viewport = new Viewport("one\ntwo\nthree", 10, 2);

        assertThat(viewport.view().split("\n", -1)).hasSize(2);
    }

    @Test
    void contentShorterThanTheViewportIsPaddedToItsHeight() {
        Viewport viewport = new Viewport("one", 10, 3);

        String[] rows = viewport.view().split("\n", -1);
        assertThat(rows).hasSize(3);
        assertThat(rows[0]).startsWith("one");
    }

    @Test
    void lineWiderThanTheViewportIsCutInsteadOfWrapped() {
        Viewport viewport = new Viewport("abcdefghijklmnopqrstuvwxyz", 10, 1);

        String[] rows = viewport.view().split("\n", -1);
        assertThat(rows).containsExactly("abcdefghij");
    }

    @Test
    void renderingLeavesTheCallersStyleUnsized() {
        Style style = Style.newStyle().paddingLeft(1);
        Viewport viewport = new Viewport("one", 10, 2);
        viewport.setStyle(style);

        viewport.view();

        assertThat(style.render("x")).isEqualTo(" x");
    }
}
