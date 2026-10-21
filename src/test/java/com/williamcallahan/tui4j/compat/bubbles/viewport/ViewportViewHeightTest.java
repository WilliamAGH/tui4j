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
    void lineWiderThanTheViewportDoesNotDisplaceTheFollowingRow() {
        Viewport viewport = new Viewport("abcdefghijklmnopqrstuvwxyz\nsecond", 10, 2);

        String[] rows = viewport.view().split("\n", -1);
        assertThat(rows).containsExactly("abcdefghij", "second    ");
    }

    /** Keeps padding within positive bounds even when no content area remains. */
    @Test
    void frameConsumingTheAreaDoesNotOverflowTheViewport() {
        Viewport viewport = new Viewport("x", 1, 1);

        for (int padding : new int[] {1, 2}) {
            viewport.setStyle(Style.newStyle().paddingLeft(padding));
            assertThat(viewport.view()).isEqualTo(" ");

            viewport.setStyle(Style.newStyle().paddingTop(padding));
            assertThat(viewport.view()).isEqualTo(" ");
        }
    }

    /** Retains caller maximums that are smaller than the viewport dimensions. */
    @Test
    void renderingPreservesStricterCallerMaximums() {
        Viewport viewport = new Viewport("one\ntwo", 10, 2);
        viewport.setStyle(Style.newStyle().maxWidth(1).maxHeight(1));

        assertThat(viewport.view()).isEqualTo("o");
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
