package com.williamcallahan.tui4j.input;

import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Converts scroll events into selection updates.
 * is held near the top/bottom edge.
 * <p>
 * tui4j extension; no Bubble Tea equivalent.
 * tui4j: src/main/java/com/williamcallahan/tui4j/input/MouseSelectionAutoScroller.java
 */
public final class MouseSelectionAutoScroller {

    private final IntSupplier terminalHeight;
    private final MouseSelectionTracker selectionTracker;
    private final Consumer<Message> messageConsumer;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;

    private volatile boolean enabled;
    private volatile int edgeRows = 1;
    private volatile int intervalMs = 50;

    private volatile boolean alt;
    private volatile boolean ctrl;

    /**
     * Creates an auto scroller for mouse selection.
     *
     * @param terminalHeight terminal height supplier
     * @param selectionTracker selection tracker
     * @param messageConsumer message sink for synthesized scroll events
     */
    public MouseSelectionAutoScroller(
        IntSupplier terminalHeight,
        MouseSelectionTracker selectionTracker,
        Consumer<Message> messageConsumer
    ) {
        this.terminalHeight = Objects.requireNonNull(
            terminalHeight,
            "terminalHeight"
        );
        this.selectionTracker = Objects.requireNonNull(
            selectionTracker,
            "selectionTracker"
        );
        this.messageConsumer = Objects.requireNonNull(
            messageConsumer,
            "messageConsumer"
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tui4j-MouseSelection-Autoscroll-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Enables auto scrolling with the current configuration.
     */
    public void enable() {
        enabled = true;
    }

    /**
     * Enables auto scrolling and configures edge and interval settings.
     *
     * @param edgeRows number of rows from the edge to trigger auto scroll
     * @param intervalMs polling interval in milliseconds
     */
    public void configure(int edgeRows, int intervalMs) {
        enabled = true;
        this.edgeRows = Math.max(edgeRows, 1);
        this.intervalMs = Math.max(intervalMs, 10);
    }

    /**
     * Starts the auto scroll scheduler if enabled.
     */
    public void start() {
        if (!enabled) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        scheduler.scheduleAtFixedRate(
            this::tick,
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the auto scroll scheduler.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Updates modifier state based on the current mouse message.
     *
     * @param message mouse message
     */
    public void onMouse(
        com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage message
    ) {
        updateModifiers(message);
    }

    /**
     * Handles a mouse message by updating modifier state.
     *
     * @param message mouse message
     */
    public void onMouseMessage(
        com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage message
    ) {
        updateModifiers(message);
    }

    private void tick() {
        if (!enabled || !running.get()) {
            return;
        }
        if (!selectionTracker.isSelecting()) {
            return;
        }

        int height = terminalHeight.getAsInt();
        if (height <= 0) {
            return;
        }

        int row = selectionTracker.lastRow();
        int topEdge = edgeRows - 1;
        int bottomEdge = height - edgeRows;

        MouseButton wheel = null;
        if (row <= topEdge) {
            wheel = MouseButton.MouseButtonWheelUp;
        } else if (row >= bottomEdge) {
            wheel = MouseButton.MouseButtonWheelDown;
        }
        if (wheel == null) {
            return;
        }

        messageConsumer.accept(
            new MouseMessage(
                selectionTracker.lastColumn(),
                row,
                // Never shifted: Viewport, like upstream, pans sideways on a shifted wheel.
                false,
                alt,
                ctrl,
                MouseAction.MouseActionPress,
                wheel
            )
        );
    }

    private void updateModifiers(
        com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage message
    ) {
        if (message == null) {
            return;
        }
        // Keep modifier state consistent with the latest user interaction.
        alt = message.isAlt();
        ctrl = message.isCtrl();
    }
}
