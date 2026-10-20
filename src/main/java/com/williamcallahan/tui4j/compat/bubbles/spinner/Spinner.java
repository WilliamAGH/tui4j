package com.williamcallahan.tui4j.compat.bubbles.spinner;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.lipgloss.Style;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Port of Bubbles spinner.
 * Bubble Tea: bubbletea/examples/spinner/main.go
 */
public class Spinner implements Model {

    /**
     * Sequence for per-instance spinner identifiers.
     * <p>
     * Upstream keeps a package counter so each spinner only consumes the tick
     * messages it produced itself (bubbles/spinner/spinner.go nextID).
     */
    private static final AtomicInteger LAST_ID = new AtomicInteger(0);

    private SpinnerType type;
    private int frame;
    private final int id;
    private int tag;
    private Style style = Style.newStyle();

    /**
     * Creates a spinner with the given type.
     *
     * @param type spinner type
     */
    public Spinner(SpinnerType type) {
        this.type = type;
        this.id = nextId();
    }

    /**
     * Sets the spinner type.
     *
     * @param type spinner type
     */
    public void setType(SpinnerType type) {
        this.type = type;
    }

    /**
     * Sets the style for this spinner.
     *
     * @param style style to apply
     * @return this spinner
     */
    public Spinner setStyle(Style style) {
        this.style = style;
        return this;
    }

    public Command init() {
        return this::tick;
    }

    public UpdateResult<Spinner> update(Message msg) {
        if (msg instanceof TickMessage tickMessage) {
            if (tickMessage.id() > 0 && tickMessage.id() != id) {
                return UpdateResult.from(this);
            }
            if (tickMessage.tag() > 0 && tickMessage.tag() != tag) {
                return UpdateResult.from(this);
            }

            frame++;
            if (frame >= type.frames().length) {
                frame = 0;
            }

            tag++;
            return UpdateResult.from(
                    this,
                    Command.tick(type.duration(), localDateTime -> new TickMessage(localDateTime, tag, id))
            );
        }

        return UpdateResult.from(this);
    }

    public String view() {
        if (frame >= type.frames().length) {
            return "(error)";
        }
        return style.render(type.frames()[frame]);
    }

    /**
     * Produces an initial tick message.
     *
     * @return tick message
     */
    public Message tick() {
        return new TickMessage(LocalDateTime.now(), tag, id);
    }

    /**
     * Allocates the next process-wide spinner identifier, so every spinner instance
     * receives a distinct id that scopes tick messages to its own producer.
     *
     * @return unique spinner id
     */
    private static int nextId() {
        return LAST_ID.incrementAndGet();
    }
}
