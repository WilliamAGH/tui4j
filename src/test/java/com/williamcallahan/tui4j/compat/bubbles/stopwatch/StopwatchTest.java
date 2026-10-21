package com.williamcallahan.tui4j.compat.bubbles.stopwatch;

import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Stopwatch.
 */
class StopwatchTest {

    @Test
    void testInitialState() {
        Stopwatch stopwatch = new Stopwatch();
        assertThat(stopwatch.running()).isFalse();
        assertThat(stopwatch.elapsed()).isEqualTo(Duration.ZERO);
        assertThat(stopwatch.interval()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void testCustomInterval() {
        Duration interval = Duration.ofMillis(100);
        Stopwatch stopwatch = new Stopwatch(interval);
        assertThat(stopwatch.interval()).isEqualTo(interval);
    }

    @Test
    void testStartStopMessageUpdatesRunningState() {
        Stopwatch stopwatch = new Stopwatch();
        assertThat(stopwatch.running()).isFalse();

        stopwatch.update(new StartStopMessage(stopwatch.id(), true));
        assertThat(stopwatch.running()).isTrue();

        stopwatch.update(new StartStopMessage(stopwatch.id(), false));
        assertThat(stopwatch.running()).isFalse();
    }

    @Test
    void testStartCommandDoesNotMutateState() {
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();

        assertThat(stopwatch.running()).isFalse();
    }

    @Test
    void testStopCommandDoesNotMutateState() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));

        stopwatch.stop();

        assertThat(stopwatch.running()).isTrue();
    }

    @Test
    void testResetCommandDoesNotMutateElapsed() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));
        stopwatch.update(new TickMessage(stopwatch.id(), 0));

        stopwatch.reset();

        assertThat(stopwatch.elapsed()).isNotEqualTo(Duration.ZERO);
    }

    @Test
    void testStartStopMessageFromDifferentIdIsIgnored() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.update(new StartStopMessage(999, true));
        assertThat(stopwatch.running()).isFalse();
    }

    @Test
    void testTickIncrementsElapsed() {
        Stopwatch stopwatch = new Stopwatch(Duration.ofSeconds(1));
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));

        Duration before = stopwatch.elapsed();
        stopwatch.update(new TickMessage(stopwatch.id(), 0));

        assertThat(stopwatch.elapsed()).isEqualTo(before.plus(Duration.ofSeconds(1)));
    }

    @Test
    void testTickIgnoredWhenStopped() {
        Stopwatch stopwatch = new Stopwatch(Duration.ofSeconds(1));
        Duration before = stopwatch.elapsed();
        stopwatch.update(new TickMessage(stopwatch.id(), 0));

        assertThat(stopwatch.elapsed()).isEqualTo(before);
    }

    @Test
    void testTickWithDifferentIdIsIgnored() {
        Stopwatch stopwatch = new Stopwatch(Duration.ofSeconds(1));
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));

        Duration before = stopwatch.elapsed();
        stopwatch.update(new TickMessage(999, 0));

        assertThat(stopwatch.elapsed()).isEqualTo(before);
    }

    @Test
    void testTickWithWrongTagIsIgnored() {
        Stopwatch stopwatch = new Stopwatch(Duration.ofSeconds(1));
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));

        stopwatch.update(new TickMessage(stopwatch.id(), 0));
        Duration afterFirstTick = stopwatch.elapsed();

        stopwatch.update(new TickMessage(stopwatch.id(), 999));

        assertThat(stopwatch.elapsed()).isEqualTo(afterFirstTick);
    }

    @Test
    void testResetResetsElapsed() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));
        stopwatch.update(new TickMessage(stopwatch.id(), 0));
        stopwatch.update(new TickMessage(stopwatch.id(), 1));

        stopwatch.update(new ResetMessage(stopwatch.id()));

        assertThat(stopwatch.elapsed()).isEqualTo(Duration.ZERO);
    }

    @Test
    void testResetWithDifferentIdIsIgnored() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));
        stopwatch.update(new TickMessage(stopwatch.id(), 0));

        stopwatch.update(new ResetMessage(999));

        assertThat(stopwatch.elapsed()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void testTagIncrementsOnTick() {
        Stopwatch stopwatch = new Stopwatch(Duration.ofMillis(100));
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));

        UpdateResult<Stopwatch> result1 = stopwatch.update(new TickMessage(stopwatch.id(), 0));
        assertThat(result1.command()).isNotNull();

        stopwatch.update(new TickMessage(stopwatch.id(), 1));
        UpdateResult<Stopwatch> result2 = stopwatch.update(new TickMessage(stopwatch.id(), 2));

        TickMessage tickMsg = (TickMessage) result2.command().execute();
        assertThat(tickMsg.tag()).isEqualTo(3);
    }

    @Test
    void testUniqueIds() {
        Stopwatch stopwatch1 = new Stopwatch();
        Stopwatch stopwatch2 = new Stopwatch();
        assertThat(stopwatch1.id()).isNotEqualTo(stopwatch2.id());
    }

    /**
     * Upstream {@code stopwatch.Update} matches {@code msg.ID != m.id} strictly,
     * unlike the spinner ({@code ID > 0}) and timer ({@code ID != 0}) which accept
     * an unset-id sentinel: broadcasting hand-built id-0 messages must not drive
     * a stopwatch.
     */
    @Test
    void testStartStopMessageWithUnsetIdIsIgnored() {
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.update(new StartStopMessage(0, true));

        assertThat(stopwatch.running()).isFalse();
    }

    /**
     * A broadcast id-0 tick must not advance the elapsed time; upstream rejects
     * it because {@code msg.ID != m.id} and component ids start at 1.
     */
    @Test
    void testTickMessageWithUnsetIdIsIgnored() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));
        Duration before = stopwatch.elapsed();

        stopwatch.update(new TickMessage(0, 0));

        assertThat(stopwatch.elapsed()).isEqualTo(before);
    }

    /**
     * A broadcast id-0 reset must not clear the elapsed time; upstream matches
     * the reset id strictly.
     */
    @Test
    void testResetMessageWithUnsetIdIsIgnored() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.update(new StartStopMessage(stopwatch.id(), true));
        stopwatch.update(new TickMessage(stopwatch.id(), 0));

        stopwatch.update(new ResetMessage(0));

        assertThat(stopwatch.elapsed()).isNotEqualTo(Duration.ZERO);
    }
}
