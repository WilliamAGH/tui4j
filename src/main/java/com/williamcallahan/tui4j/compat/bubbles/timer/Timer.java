package com.williamcallahan.tui4j.compat.bubbles.timer;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.duration.GoDuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Countdown timer model (ported from charmbracelet/bubbles).
 * Bubble Tea: bubbletea/examples/timer/main.go
 */
public class Timer implements Model {

    private static final Duration DEFAULT_TIMEOUT = Duration.ZERO;
    private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(1);

    private static final AtomicInteger LAST_ID = new AtomicInteger(0);

    private Duration timeout;
    private Duration interval;
    private int id;
    private int tag;
    private boolean running;

    /**
     * Creates a timer with default timeout and interval.
     */
    public Timer() {
        this(DEFAULT_TIMEOUT, DEFAULT_INTERVAL);
    }

    /**
     * Creates a timer with the specified timeout and default interval.
     *
     * @param timeout countdown duration
     */
    public Timer(Duration timeout) {
        this(timeout, DEFAULT_INTERVAL);
    }

    /**
     * Creates a timer with the specified timeout and interval.
     *
     * @param timeout countdown duration
     * @param interval tick interval
     */
    public Timer(Duration timeout, Duration interval) {
        this.timeout = timeout;
        this.interval = interval;
        this.running = true;
        this.id = nextId();
    }

    /**
     * Returns the timer ID.
     *
     * @return timer ID
     */
    public int id() {
        return id;
    }

    /**
     * Returns the remaining timeout.
     *
     * @return remaining duration
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Sets the timeout duration.
     *
     * @param timeout duration to set
     */
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the tick interval.
     *
     * @return tick interval
     */
    public Duration interval() {
        return interval;
    }

    /**
     * Sets the tick interval.
     *
     * @param interval interval to set
     */
    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    /**
     * Returns whether the timer is running.
     *
     * @return true if running
     */
    public boolean running() {
        return !timedout() && running;
    }

    /**
     * Returns whether the timer has timed out.
     *
     * @return true if timed out
     */
    public boolean timedout() {
        return timeout.isZero() || timeout.isNegative();
    }

    @Override
    public Command init() {
        return tick();
    }

    @Override
    public UpdateResult<Timer> update(Message msg) {
        if (msg instanceof StartStopMessage startStopMessage) {
            if (startStopMessage.id() != 0 && startStopMessage.id() != id) {
                return UpdateResult.from(this);
            }
            running = startStopMessage.running();
            return UpdateResult.from(this, tick());
        }
        if (msg instanceof TickMessage tickMessage) {
            if (!running() || (tickMessage.id() != 0 && tickMessage.id() != id)) {
                return UpdateResult.from(this);
            }
            if (tickMessage.tag() > 0 && tickMessage.tag() != tag) {
                return UpdateResult.from(this);
            }

            timeout = timeout.minus(interval);
            Command timedout = timedoutCommand();
            if (timedout != null) {
                return UpdateResult.from(this, Command.batch(tick(), timedout));
            }
            return UpdateResult.from(this, tick());
        }

        return UpdateResult.from(this);
    }

    /**
     * Renders the remaining timeout.
     * <p>
     * Upstream renders {@code m.Timeout.String()}, so this delegates to the shared
     * Go duration formatter ({@code 1h2m3s}, {@code 500ms}).
     *
     * @return formatted timeout
     */
    @Override
    public String view() {
        return GoDuration.format(timeout);
    }

    /**
     * Starts the timer.
     *
     * @return start command
     */
    public Command start() {
        return startStop(true);
    }

    /**
     * Stops the timer.
     *
     * @return stop command
     */
    public Command stop() {
        return startStop(false);
    }

    /**
     * Toggles the timer running state.
     *
     * @return toggle command
     */
    public Command toggle() {
        return startStop(!running());
    }

    private static int nextId() {
        return LAST_ID.incrementAndGet();
    }

    private Command tick() {
        return Command.tick(interval, __ -> new TickMessage(id, timedout(), tag));
    }

    private Command timedoutCommand() {
        if (!timedout()) {
            return null;
        }
        return () -> new TimeoutMessage(id);
    }

    private Command startStop(boolean running) {
        return () -> new StartStopMessage(id, running);
    }
}
