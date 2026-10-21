package com.williamcallahan.tui4j.compat.bubbles.viewport;

import static org.assertj.core.api.Assertions.assertThat;

import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import org.junit.jupiter.api.Test;

/**
 * Verifies mouse-wheel scrolling as ported from {@code bubbles/viewport/viewport.go}.
 * SGR button codes: 64 wheel up, 65 wheel down, 66 wheel left, 67 wheel right; +4 adds shift.
 */
class ViewportMouseWheelTest {

    private static final String TALL_CONTENT = "one\ntwo\nthree\nfour\nfive\nsix\nseven\neight";
    private static final String WIDE_CONTENT = "abcdefghijklmnopqrstuvwxyz\nsecond";

    /** Verifies vertical wheel scrolling and its bounds. */
    @Test
    void wheelScrollsVerticallyByTheWheelDeltaWithinBounds() {
        Viewport viewport = new Viewport(TALL_CONTENT, 20, 3);

        viewport.update(wheel(65));
        assertThat(viewport.getYOffset()).isEqualTo(3);
        assertThat(viewport.view()).startsWith("four");

        viewport.update(wheel(65));
        assertThat(viewport.getYOffset()).isEqualTo(5);

        viewport.update(wheel(64));
        assertThat(viewport.getYOffset()).isEqualTo(2);
    }

    /** Verifies the configured number of rows per wheel step. */
    @Test
    void customWheelDeltaSetsTheRowsPerStep() {
        Viewport viewport = new Viewport(TALL_CONTENT, 20, 3);
        viewport.setMouseWheelDelta(1);

        viewport.update(wheel(65));

        assertThat(viewport.getYOffset()).isEqualTo(1);
    }

    /** Verifies that disabling the wheel preserves the visible content. */
    @Test
    void disabledWheelLeavesTheViewInPlace() {
        Viewport viewport = new Viewport(TALL_CONTENT, 20, 3);
        viewport.setMouseWheelEnabled(false);

        viewport.update(wheel(65));

        assertThat(viewport.getYOffset()).isZero();
    }

    /** Verifies horizontal and shifted wheel panning. */
    @Test
    void horizontalAndShiftedWheelPanByTheHorizontalStep() {
        Viewport viewport = new Viewport(WIDE_CONTENT, 10, 2);
        viewport.setHorizontalStep(4);

        viewport.update(wheel(67));
        assertThat(viewport.getXOffset()).isEqualTo(4);

        viewport.update(wheel(69));
        assertThat(viewport.getXOffset()).isEqualTo(8);
        assertThat(viewport.getYOffset()).isZero();

        viewport.update(wheel(68));
        viewport.update(wheel(66));
        assertThat(viewport.getXOffset()).isZero();
    }

    /** Verifies that non-wheel presses leave the viewport in place. */
    @Test
    void nonWheelButtonsDoNotScroll() {
        Viewport viewport = new Viewport(TALL_CONTENT, 20, 3);

        viewport.update(MouseMessage.parseSGRMouseEvent(0, 1, 1, false));

        assertThat(viewport.getYOffset()).isZero();
    }

    /** Creates a wheel press from its SGR button code. */
    private static MouseMessage wheel(int sgrButton) {
        return MouseMessage.parseSGRMouseEvent(sgrButton, 1, 1, false);
    }
}
