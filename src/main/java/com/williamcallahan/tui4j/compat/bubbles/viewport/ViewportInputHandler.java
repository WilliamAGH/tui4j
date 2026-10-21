package com.williamcallahan.tui4j.compat.bubbles.viewport;

import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;

/**
 * Applies key and mouse-wheel messages to a {@link Viewport}.
 * <p>
 * Port of {@code updateAsModel} in {@code bubbles/viewport/viewport.go}. Upstream's
 * high-performance {@code ViewUp}/{@code ViewDown} commands are not ported, so every
 * scroll only moves the offsets.
 */
final class ViewportInputHandler {

    private ViewportInputHandler() {
    }

    /**
     * Scrolls the viewport for a bound key press or a mouse-wheel press.
     *
     * @param viewport viewport to scroll
     * @param msg incoming message
     */
    static void apply(Viewport viewport, Message msg) {
        if (msg instanceof KeyPressMessage keyPress) {
            applyKey(viewport, keyPress);
        } else if (msg instanceof MouseMessage mouse) {
            applyWheel(viewport, mouse);
        }
    }

    /** Scrolls for the first key binding the press matches. */
    private static void applyKey(Viewport viewport, KeyPressMessage keyPress) {
        KeyMap keyMap = viewport.getKeyMap();
        if (Binding.matches(keyPress, keyMap.pageDown())) {
            viewport.pageDown();
        } else if (Binding.matches(keyPress, keyMap.pageUp())) {
            viewport.pageUp();
        } else if (Binding.matches(keyPress, keyMap.halfPageDown())) {
            viewport.halfPageDown();
        } else if (Binding.matches(keyPress, keyMap.halfPageUp())) {
            viewport.halfPageUp();
        } else if (Binding.matches(keyPress, keyMap.down())) {
            viewport.scrollDown(1);
        } else if (Binding.matches(keyPress, keyMap.up())) {
            viewport.scrollUp(1);
        } else if (Binding.matches(keyPress, keyMap.left())) {
            viewport.scrollLeft(viewport.horizontalStep());
        } else if (Binding.matches(keyPress, keyMap.right())) {
            viewport.scrollRight(viewport.horizontalStep());
        }
    }

    /**
     * Scrolls on wheel presses; shift turns the vertical wheel into horizontal panning,
     * as upstream does, because some terminals send no horizontal wheel buttons.
     */
    private static void applyWheel(Viewport viewport, MouseMessage mouse) {
        if (!viewport.isMouseWheelEnabled() || mouse.getAction() != MouseAction.MouseActionPress) {
            return;
        }
        switch (mouse.getButton()) {
            case MouseButtonWheelUp -> {
                if (mouse.isShift()) {
                    viewport.scrollLeft(viewport.horizontalStep());
                } else {
                    viewport.scrollUp(viewport.getMouseWheelDelta());
                }
            }
            case MouseButtonWheelDown -> {
                if (mouse.isShift()) {
                    viewport.scrollRight(viewport.horizontalStep());
                } else {
                    viewport.scrollDown(viewport.getMouseWheelDelta());
                }
            }
            case MouseButtonWheelLeft -> viewport.scrollLeft(viewport.horizontalStep());
            case MouseButtonWheelRight -> viewport.scrollRight(viewport.horizontalStep());
            default -> {
            }
        }
    }
}
