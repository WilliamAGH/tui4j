package com.williamcallahan.tui4j.compat.bubbles.progress;

/**
 * Port of the progress spring helper.
 * Upstream: github.com/charmbracelet/bubbles/progress (spring field)
 * <p>
 * Bubbles: progress/progress.go.
 * <p>
 * Upstream builds the spring with
 * {@code harmonica.NewSpring(harmonica.FPS(fps), frequency, damping)}, so the
 * damped-oscillator physics are owned by the canonical
 * {@link com.williamcallahan.tui4j.compat.harmonica.Spring} port. This type only
 * adapts that owner's {@code double[]} result to the named
 * {@link SpringUpdateResult} the progress animator consumes; it deliberately
 * holds no physics of its own (a local Euler step diverged from upstream's
 * closed-form solution from the very first frame).
 */
public class Spring {

    /**
     * Port of the spring update strategy.
     * Upstream: github.com/charmbracelet/bubbles/progress (springUpdateFn)
     * <p>
     * Bubbles: progress/progress.go.
     */
    public interface SpringUpdate {
        /**
         * Applies an incoming message and returns the next model state.
         *
         * @param position position
         * @param velocity velocity
         * @param target target
         * @return next model state and command
         */
        SpringUpdateResult update(
            double position,
            double velocity,
            double target
        );
    }

    /**
     * Compatibility port of SpringUpdateResult to preserve upstream behavior.
     * <p>
     * Bubbles: progress/progress.go.
     *
     * @param position updated spring position
     * @param velocity updated spring velocity
     */
    public record SpringUpdateResult(double position, double velocity) {}

    private static final int FPS = 60;

    private final com.williamcallahan.tui4j.compat.harmonica.Spring delegate;

    /**
     * Creates Spring to keep this component ready for use, stepping at the
     * default 60 frames per second.
     *
     * @param frequency spring frequency
     * @param damping spring damping
     */
    public Spring(double frequency, double damping) {
        this(frequency, damping, com.williamcallahan.tui4j.compat.harmonica.Spring.fps(FPS));
    }

    private Spring(double frequency, double damping, double deltaTime) {
        this.delegate = com.williamcallahan.tui4j.compat.harmonica.Spring
                .newSpring(deltaTime, frequency, damping);
    }

    /**
     * Applies an incoming message and returns the next model state.
     *
     * @param position position
     * @param velocity velocity
     * @param target target
     * @return next model state and command
     */
    public SpringUpdateResult update(
        double position,
        double velocity,
        double target
    ) {
        double[] updated = delegate.update(position, velocity, target);
        return new SpringUpdateResult(updated[0], updated[1]);
    }

    /**
     * Creates a spring using the legacy FPS signature.
     *
     * @param fps frames per second; one update advances the simulation by
     *            {@code 1 / fps} seconds
     * @param frequency spring frequency
     * @param damping spring damping
     * @return spring instance
     */
    public static Spring withFPS(double fps, double frequency, double damping) {
        return new Spring(frequency, damping, 1.0 / fps);
    }
}
