package com.williamcallahan.tui4j.compat.bubbletea;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

/**
 * Timer-based command implementations for {@link Command#tick} and {@link Command#every}.
 * <p>
 * Port of github.com/charmbracelet/bubbletea/commands.go (tick/every).
 */
final class CommandTimer {

    private CommandTimer() {
    }

    /**
     * Emits a message after a duration.
     *
     * @param duration delay duration
     * @param fn function to map time to message
     * @return tick command
     */
    static Command tick(Duration duration, Function<LocalDateTime, Message> fn) {
        return () -> {
            BlockingQueue<LocalDateTime> queue = new ArrayBlockingQueue<>(1);
            Timer timer = new Timer(true);

            timer.schedule(new TimeEnqueueTask(queue), duration.toMillis());

            try {
                LocalDateTime time = queue.take();
                return fn.apply(time);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Tick command interrupted", e);
            } finally {
                timer.cancel();
            }
        };
    }

    /**
     * Emits a message aligned to the system clock.
     *
     * @param duration tick duration
     * @param fn function to map time to message
     * @return every command
     */
    static Command every(Duration duration, Function<LocalDateTime, Message> fn) {
        return () -> {
            long millis = duration.toMillis();
            if (millis <= 0) {
                return fn.apply(LocalDateTime.now());
            }

            long now = System.currentTimeMillis();
            long next = ((now / millis) + 1) * millis;
            long delay = Math.max(0, next - now);

            BlockingQueue<LocalDateTime> queue = new ArrayBlockingQueue<>(1);
            Timer timer = new Timer(true);
            timer.schedule(new TimeEnqueueTask(queue), delay);

            try {
                LocalDateTime time = queue.take();
                return fn.apply(time);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Every command interrupted", e);
            } finally {
                timer.cancel();
            }
        };
    }

    /**
     * Enqueues the current time for timer-based commands.
     */
    static final class TimeEnqueueTask extends TimerTask {

        private final BlockingQueue<LocalDateTime> queue;

        /**
         * Creates a timer task that delivers a single timestamp.
         *
         * @param queue queue receiving the timestamp
         */
        TimeEnqueueTask(BlockingQueue<LocalDateTime> queue) {
            this.queue = queue;
        }

        /** Delivers the current time to the waiting command. */
        @Override
        public void run() {
            queue.add(LocalDateTime.now());
        }
    }
}
