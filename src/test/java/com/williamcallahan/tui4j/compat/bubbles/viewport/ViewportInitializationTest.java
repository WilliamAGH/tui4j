package com.williamcallahan.tui4j.compat.bubbles.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import org.junit.jupiter.api.Test;

/** Verifies that the first message preserves configuration supplied by callers. */
class ViewportInitializationTest {

    /** An unbound page key must remain unbound when processing the first message. */
    @Test
    void firstUpdatePreservesCustomKeyMap() {
        KeyMap keyMap = new KeyMap();
        keyMap.pageDown().unbind();
        Viewport viewport = new Viewport("one\ntwo\nthree\nfour", 20, 2, keyMap);

        viewport.update(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[] {'f'})));

        assertEquals(0, viewport.getYOffset());
    }

    /** A wheel message must preserve the caller's disabled wheel setting. */
    @Test
    void firstUpdatePreservesDisabledMouseWheel() {
        Viewport viewport = new Viewport("one\ntwo\nthree\nfour", 20, 2);
        viewport.setMouseWheelEnabled(false);

        viewport.update(MouseMessage.parseSGRMouseEvent(65, 1, 1, false));

        assertFalse(viewport.isMouseWheelEnabled());
    }

    /** Custom wheel speed must survive the first message just like wheel enablement. */
    @Test
    void firstUpdatePreservesMouseWheelDelta() {
        Viewport viewport = new Viewport("one\ntwo\nthree\nfour", 20, 2);
        viewport.setMouseWheelDelta(7);

        viewport.update(MouseMessage.parseSGRMouseEvent(65, 1, 1, false));

        assertEquals(7, viewport.getMouseWheelDelta());
    }
}
