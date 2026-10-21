package com.williamcallahan.tui4j.compat.bubbles.cursor;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of github.com/charmbracelet/bubbles/cursor/cursor_test.go.
 */
class CursorTest {

    @Test
    void testBlinkCommandCapturesTag() {
        Cursor cursor = new Cursor();
        cursor.setBlinkSpeed(Duration.ofMillis(2));

        Command first = cursor.blinkCommand();
        Command second = cursor.blinkCommand();

        Message firstMsg = first.execute();
        Message secondMsg = second.execute();

        assertThat(firstMsg).isInstanceOf(BlinkMessage.class);
        assertThat(secondMsg).isInstanceOf(BlinkMessage.class);

        BlinkMessage firstBlink = (BlinkMessage) firstMsg;
        BlinkMessage secondBlink = (BlinkMessage) secondMsg;

        assertThat(firstBlink.tag()).isEqualTo(1);
        assertThat(secondBlink.tag()).isEqualTo(2);
    }

    /**
     * Two cursors sit on the same blink tag, so only the per-instance id can tell
     * them apart; a blink produced by one cursor must not advance the other.
     */
    @Test
    void testBlinkMessageIsScopedToItsOwningCursor() {
        Cursor owner = new Cursor();
        Cursor other = new Cursor();
        owner.setBlinkSpeed(Duration.ofMillis(2));
        other.setBlinkSpeed(Duration.ofMillis(2));

        // Both cursors are expecting tag 1, so only the cursor id can separate them.
        Command ownerBlink = owner.blinkCommand();
        other.blinkCommand();

        Message fromOwner = ownerBlink.execute();

        UpdateResult<Cursor> foreign = other.update(fromOwner);
        assertThat(foreign.command())
                .as("a cursor ignores a blink message produced by another cursor")
                .isNull();

        assertThat(owner.update(fromOwner).command())
                .as("a cursor still accepts its own blink message")
                .isNotNull();
    }
}
