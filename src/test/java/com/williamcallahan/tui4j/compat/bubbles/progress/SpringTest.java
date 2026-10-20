package com.williamcallahan.tui4j.compat.bubbles.progress;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests the progress spring helper.
 * <p>
 * Upstream: bubbles/progress/progress.go, which builds its spring with
 * {@code harmonica.NewSpring(harmonica.FPS(fps), frequency, damping)}, so the frame
 * rate defines the integration step {@code 1 / fps}.
 * <p>
 * Expected positions and velocities are the values the upstream harmonica spring
 * produces for frequency 18, damping 1 (critically damped), starting at rest at
 * position 0 toward target 1. Harmonica solves the oscillator in closed form, so
 * the first frame moves about 0.037 cells, not the 0.09 a naive Euler step gives.
 * The tolerance covers the upstream {@code FPS} integer-nanosecond truncation
 * (16666666ns) versus this port's exact {@code 1.0 / 60} step.
 */
class SpringTest {

    private static final double FREQUENCY = 18.0;
    private static final double DAMPING = 1.0;

    @Test
    void testWithFpsUsesTheRequestedFrameRate() {
        // when
        Spring.SpringUpdateResult thirtyFps = Spring.withFPS(30.0, FREQUENCY, DAMPING).update(0, 0, 1);
        Spring.SpringUpdateResult sixtyFps = Spring.withFPS(60.0, FREQUENCY, DAMPING).update(0, 0, 1);

        // then: one 30 fps step covers about the distance of two 60 fps steps
        // (the closed-form solution is a semigroup; only upstream's truncated
        // frame time makes the two differ in the last digits).
        assertThat(thirtyFps.position()).isCloseTo(0.121901380273835791, within(1e-6));
        assertThat(sixtyFps.position()).isCloseTo(0.036936310446821219, within(1e-6));
        assertThat(thirtyFps.velocity()).isCloseTo(5.9271656461068218, within(1e-6));
        assertThat(sixtyFps.velocity()).isCloseTo(4.0004182796695593, within(1e-6));
    }

    @Test
    void testDefaultFrameRateIsSixty() {
        // when
        Spring.SpringUpdateResult stepped = new Spring(FREQUENCY, DAMPING).update(0, 0, 1);
        Spring.SpringUpdateResult explicit = Spring.withFPS(60.0, FREQUENCY, DAMPING).update(0, 0, 1);

        // then
        assertThat(stepped.position()).isEqualTo(explicit.position());
        assertThat(stepped.velocity()).isEqualTo(explicit.velocity());
    }
}
