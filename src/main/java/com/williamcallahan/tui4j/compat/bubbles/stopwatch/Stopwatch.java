package com.williamcallahan.tui4j.compat.bubbles.stopwatch;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.duration.GoDuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stopwatch bubble component.
 * Port of charmbracelet/bubbles/stopwatch.
 */
public class Stopwatch implements Model {

    private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(1);

    private static final AtomicInteger LAST_ID = new AtomicInteger(0);

    private Duration elapsed;
    private Duration interval;
    private int id;
    private int tag;
    private boolean running;

    /**
     * Creates a stopwatch with the default interval of one second.
     */
    public Stopwatch() {
        this(DEFAULT_INTERVAL);
    }

    /**
     * Creates a stopwatch with the specified interval.
     *
     * @param interval tick interval
     */
    public Stopwatch(Duration interval) {
        this.elapsed = Duration.ZERO;
        this.interval = interval;
        this.running = false;
        this.id = nextId();
    }

    /**
     * Returns the unique ID for this stopwatch.
     *
     * @return stopwatch ID
     */
    public int id() {
        return id;
    }

    /**
     * Returns the elapsed duration.
     *
     * @return elapsed time
     */
    public Duration elapsed() {
        return elapsed;
    }

    /**
     * Sets the elapsed duration (for testing purposes).
     *
     * @param elapsed the elapsed duration
     */
    public void setElapsed(Duration elapsed) {
        this.elapsed = elapsed;
    }

    /**
     * Returns the tick interval.
     *
     * @return interval duration
     */
    public Duration interval() {
        return interval;
    }

    /**
     * Sets the tick interval.
     *
     * @param interval tick interval
     */
    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    /**
     * Returns whether the stopwatch is running.
     *
     * @return {@code true} when running
     */
    public boolean running() {
        return running;
    }

    @Override
    public Command init() {
        return start();
    }

    @Override
    public UpdateResult<Stopwatch> update(Message msg) {
        if (msg instanceof StartStopMessage startStopMessage) {
            return handleStartStop(startStopMessage.id(), startStopMessage.running());
        }
        if (msg instanceof ResetMessage resetMessage) {
            return handleReset(resetMessage.id());
        }
        if (msg instanceof TickMessage tickMessage) {
            return handleTick(tickMessage.id(), tickMessage.tag());
        }

        return UpdateResult.from(this);
    }

    /**
     * Renders the elapsed time.
     * <p>
     * Upstream renders {@code m.d.String()}, so this delegates to the shared
     * Go duration formatter ({@code 1h2m3s}, {@code 500ms}).
     *
     * @return formatted elapsed time
     */
    @Override
    public String view() {
        return GoDuration.format(elapsed);
    }

    /**
     * Returns a command to resume the stopwatch tick loop.
     *
     * @return start command
     */
    public Command start() {
        return () -> new StartStopMessage(id, true);
    }

    /**
     * Returns a command to halt the tick loop.
     *
     * @return stop command
     */
    public Command stop() {
        return () -> new StartStopMessage(id, false);
    }

    /**
     * Returns a command to reset the elapsed time to zero.
     *
     * @return reset command
     */
    public Command reset() {
        return () -> new ResetMessage(id);
    }

    /**
     * Returns a command to switch between running and stopped states.
     *
     * @return toggle command
     */
    public Command toggle() {
        return () -> new StartStopMessage(id, !running);
    }

    private static int nextId() {
        return LAST_ID.incrementAndGet();
    }

    private Command tick() {
        return Command.tick(interval, __ -> new TickMessage(id, tag));
    }

    /**
     * Upstream {@code stopwatch.Update} matches {@code msg.ID != m.id} strictly,
     * with no unset-id sentinel (unlike the spinner's {@code ID > 0} and the
     * timer's {@code ID != 0} guards), so broadcasting hand-built messages must
     * not drive a stopwatch instance.
     */
    private UpdateResult<Stopwatch> handleStartStop(int messageId, boolean shouldRun) {
        if (messageId != id) {
            return UpdateResult.from(this);
        }
        running = shouldRun;
        if (running) {
            return UpdateResult.from(this, tick());
        }
        return UpdateResult.from(this);
    }

    private UpdateResult<Stopwatch> handleReset(int messageId) {
        if (messageId != id) {
            return UpdateResult.from(this);
        }
        elapsed = Duration.ZERO;
        return UpdateResult.from(this);
    }

    private UpdateResult<Stopwatch> handleTick(int messageId, int messageTag) {
        if (!running || messageId != id) {
            return UpdateResult.from(this);
        }
        if (messageTag > 0 && messageTag != tag) {
            return UpdateResult.from(this);
        }

        elapsed = elapsed.plus(interval);
        tag++;
        return UpdateResult.from(this, tick());
    }
}
