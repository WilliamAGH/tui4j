package com.williamcallahan.tui4j.compat.bubbles.progress;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;

import java.time.Duration;

/**
 * Spring animation state and lifecycle for {@link Progress}.
 * <p>
 * Upstream: bubbles/progress/progress.go (animation fields, handleFrame, setPercent).
 */
final class ProgressAnimator {

    private static final double FPS = 60.0;
    private static final double DEFAULT_FREQUENCY = 18.0;
    private static final double DEFAULT_DAMPING = 1.0;

    private Spring spring;
    private boolean springCustomized;
    private double percentShown;
    private double targetPercent;
    private double velocity;

    ProgressAnimator() {
    }

    double percentShown() {
        return percentShown;
    }

    double targetPercent() {
        return targetPercent;
    }

    /**
     * Returns whether the spring animation is active.
     * <p>
     * Mirrors upstream {@code progress.IsAnimating}, which compares the
     * <em>signed</em> velocity ({@code dist < 0.001 && velocity < 0.01}): an
     * under-damped spring that crosses back within 0.001 of the target while
     * moving downward has stopped animating, even though its speed is still
     * above 0.01.
     */
    boolean isAnimating() {
        double dist = Math.abs(percentShown - targetPercent);
        return !(dist < 0.001 && velocity < 0.01);
    }

    /** Returns whether spring options have been explicitly configured. */
    boolean isSpringCustomized() {
        return springCustomized;
    }

    /** Sets custom spring animation parameters. */
    void setSpringOptions(double frequency, double damping) {
        this.spring = new Spring(frequency, damping);
        this.springCustomized = true;
    }

    /**
     * Handles a frame tick for a progress model.
     *
     * @param progress the owning progress model
     * @param frameId frame progress id
     * @param frameTag frame animation tag
     * @return update result
     */
    UpdateResult<Progress> handleFrame(Progress progress, int frameId, int frameTag) {
        if (frameId != progress.id() || frameTag != progress.tag()) {
            return UpdateResult.from(progress);
        }

        if (!isAnimating()) {
            return UpdateResult.from(progress);
        }

        if (spring == null) {
            this.spring = new Spring(DEFAULT_FREQUENCY, DEFAULT_DAMPING);
        }

        Spring.SpringUpdateResult result = spring.update(percentShown, velocity, targetPercent);
        this.percentShown = result.position();
        this.velocity = result.velocity();
        return UpdateResult.from(progress, nextFrame(progress.id(), progress.tag()));
    }

    /**
     * Sets the target percent and schedules animation.
     *
     * @param p target percent (0-1)
     * @param id progress model id
     * @param tag current animation tag (will be incremented by caller)
     * @return animation command
     */
    Command setPercent(double p, int id, int tag) {
        this.targetPercent = Math.clamp(p, 0.0, 1.0);
        return nextFrame(id, tag);
    }

    private Command nextFrame(int id, int tag) {
        return Command.tick(
                Duration.ofNanos((long) (1_000_000_000.0 / FPS)),
                time -> new FrameMessage(id, tag)
        );
    }
}
