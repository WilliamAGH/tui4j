package com.williamcallahan.tui4j.compat.bubbles.viewport;

import static org.assertj.core.api.Assertions.assertThat;

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
}
