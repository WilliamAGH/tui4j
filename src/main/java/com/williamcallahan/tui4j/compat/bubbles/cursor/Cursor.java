package com.williamcallahan.tui4j.compat.bubbles.cursor;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.bubbletea.BlurMessage;
import com.williamcallahan.tui4j.compat.bubbletea.FocusMessage;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Port of the cursor blink initialization message.
 * Upstream: github.com/charmbracelet/bubbles/cursor (initialBlinkMsg)
 */
record InitialBlinkMessage() implements Message {
}

/**
 * Port of the cursor blink message.
 * Upstream: github.com/charmbracelet/bubbles/cursor (blinkMsg)
 */
record BlinkMessage(int id, int tag) implements Message {
}

/**
 * Port of the cursor blink cancel message.
 * Upstream: github.com/charmbracelet/bubbles/cursor (blinkCanceledMsg)
 */
record BlinkCanceled() implements Message {

}

/**
 * Port of the Bubble Tea cursor model used by Bubbles inputs.
 * Upstream: github.com/charmbracelet/bubbles/cursor (Model)
 */
public class Cursor implements Model {

    private static final Duration DEFAULT_BLINK_SPEED = Duration.ofMillis(530);

    /**
     * Sequence for per-instance cursor identifiers.
     * <p>
     * Upstream tags every blink message with the cursor that produced it so a
     * cursor only consumes its own blinks (bubbles/cursor/cursor.go nextID).
     */
    private static final AtomicInteger LAST_ID = new AtomicInteger(0);

    private final int id;

    private Duration blinkSpeed;
    private Style style;
    private Style textStyle;
    private String charUnderCursor;
    private int blinkTag;
    private boolean focus;
    private boolean blink;
    private CursorMode mode;

    /**
     * Creates a cursor with default settings.
     */
    public Cursor() {
        this.id = nextId();
        this.blinkSpeed = DEFAULT_BLINK_SPEED;
        this.blink = true;
        this.focus = true;
        this.mode = CursorMode.Blink;
        this.style = Style.newStyle();
        this.textStyle = Style.newStyle();
        this.charUnderCursor = "";
    }

    /**
     * Returns the initial blink command.
     *
     * @return initial command
     */
    @Override
    public Command init() {
        return Cursor::blink;
    }

    /**
     * Updates the cursor based on incoming messages.
     *
     * @param msg message to process
     * @return update result
     */
    @Override
    public UpdateResult<Cursor> update(Message msg) {
        if (msg instanceof InitialBlinkMessage) {
            if (mode != CursorMode.Blink || !focus) {
                return UpdateResult.from(this);
            }
            return UpdateResult.from(this, blinkCommand());
        } else if (msg instanceof FocusMessage) {
            return UpdateResult.from(this, focus());
        } else if (msg instanceof BlurMessage) {
            blur();
            return UpdateResult.from(this);
        } else if (msg instanceof BlinkMessage blinkMessage) {
            if (mode != CursorMode.Blink || !focus) {
                return UpdateResult.from(this);
            }
            if (blinkMessage.id() != id || blinkMessage.tag() != blinkTag) {
                return UpdateResult.from(this);
            }
            if (mode == CursorMode.Blink) {
                this.blink = !blink;
                return UpdateResult.from(this, blinkCommand());
            }
            return UpdateResult.from(this);
        } else if (msg instanceof BlinkCanceled) {
            return UpdateResult.from(this);
        }
        return UpdateResult.from(this);
    }

    /**
     * Returns a command that toggles the cursor blink.
     *
     * @return blink command
     */
    public Command blinkCommand() {
        if (mode != CursorMode.Blink) {
            return null;
        }

        blinkTag++;
        final int currentTag = blinkTag;

        return () -> {
            try (ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()) {
                return executorService
                        .schedule(() -> new BlinkMessage(id, currentTag), blinkSpeed.toMillis(), TimeUnit.MILLISECONDS)
                        .get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new BlinkCanceled();
            } catch (ExecutionException e) {
                return new BlinkCanceled();
            }
        };
    }

    /**
     * Emits an initial blink message.
     *
     * @return blink message
     */
    public static Message blink() {
        return new InitialBlinkMessage();
    }

    /**
     * Allocates the next process-wide cursor identifier, so every cursor instance
     * receives a distinct id that scopes blink messages to its own producer.
     *
     * @return unique cursor id
     */
    private static int nextId() {
        return LAST_ID.incrementAndGet();
    }

    /**
     * Focuses the cursor and optionally resumes blinking.
     *
     * @return blink command when needed
     */
    public Command focus() {
        this.focus = true;
        this.blink = this.mode == CursorMode.Hide;
        if (mode == CursorMode.Blink) {
            return blinkCommand();
        }
        return null;
    }

    /**
     * Blurs the cursor and stops blinking.
     */
    public void blur() {
        this.focus = false;
        this.blink = true;
    }

    /**
     * Renders the cursor view.
     *
     * @return cursor view
     */
    @Override
    public String view() {
        if (blink) {
            return textStyle.inline(true).render(charUnderCursor);
        }
        return style.inline(true).reverse(true).render(charUnderCursor);
    }

    /**
     * Sets the cursor mode.
     *
     * @param mode cursor mode
     * @return blink command when needed
     */
    public Command setMode(CursorMode mode) {
        this.mode = mode;
        this.blink = (mode == CursorMode.Hide || !focus);

        if (mode == CursorMode.Blink) {
            return Cursor::blink;
        }
        return null;
    }

    /**
     * Returns the current cursor mode.
     *
     * @return cursor mode
     */
    public CursorMode mode() {
        return mode;
    }

    /**
     * Returns the blink speed.
     *
     * @return blink duration
     */
    public Duration blinkSpeed() {
        return blinkSpeed;
    }

    /**
     * Sets the blink speed.
     *
     * @param blinkSpeed blink duration
     */
    public void setBlinkSpeed(Duration blinkSpeed) {
        this.blinkSpeed = blinkSpeed;
    }

    /**
     * Sets the cursor style.
     *
     * @param style cursor style
     */
    public void setStyle(Style style) {
        this.style = style.copy();
    }

    /**
     * Sets the text style for the cursor.
     *
     * @param textStyle text style
     */
    public void setTextStyle(Style textStyle) {
        this.textStyle = textStyle.copy();
    }

    /**
     * Resets the text style to the default.
     */
    public void resetTextStyle() {
        this.textStyle = Style.newStyle();
    }

    /**
     * Sets the character under the cursor.
     *
     * @param charUnderCursor character under cursor
     */
    public void setChar(String charUnderCursor) {
        this.charUnderCursor = charUnderCursor;
    }

    /**
     * Returns whether blink is currently active.
     *
     * @return whether blinking
     */
    public boolean isBlink() {
        return blink;
    }

    /**
     * Enables or disables blinking.
     *
     * @param blink whether to blink
     */
    public void setBlink(boolean blink) {
        this.blink = blink;
    }
}
