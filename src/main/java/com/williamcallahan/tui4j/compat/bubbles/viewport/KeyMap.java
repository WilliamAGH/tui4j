package com.williamcallahan.tui4j.compat.bubbles.viewport;

import com.williamcallahan.tui4j.compat.bubbles.key.Binding;

/**
 * Port of Bubbles viewport key bindings.
 * Upstream: github.com/charmbracelet/bubbles/viewport (KeyMap)
 */
public class KeyMap {

    private final Binding pageDown;
    private final Binding pageUp;
    private final Binding halfPageDown;
    private final Binding halfPageUp;
    private final Binding down;
    private final Binding up;
    private final Binding left;
    private final Binding right;

    /**
     * Creates default viewport key bindings.
     * <p>
     * Mirrors the pager-like defaults of upstream
     * {@code viewport/keymap.go}, including the vim movement keys: pressing
     * {@code j}/{@code k}/{@code h}/{@code l} scrolls, {@code f}/{@code b}
     * page, and the half-page bindings are {@code d}/{@code ctrl+d} and
     * {@code u}/{@code ctrl+u}.
     */
    public KeyMap() {
        this.pageDown = new Binding(Binding.withKeys("pgdown", " ", "f"), Binding.withHelp("f/pgdn", "page down"));
        this.pageUp = new Binding(Binding.withKeys("pgup", "b"), Binding.withHelp("b/pgup", "page up"));
        this.halfPageDown = new Binding(Binding.withKeys("d", "ctrl+d"), Binding.withHelp("d", "½ page down"));
        this.halfPageUp = new Binding(Binding.withKeys("u", "ctrl+u"), Binding.withHelp("u", "½ page up"));
        this.down = new Binding(Binding.withKeys("down", "j"), Binding.withHelp("↓/j", "down"));
        this.up = new Binding(Binding.withKeys("up", "k"), Binding.withHelp("↑/k", "up"));
        this.left = new Binding(Binding.withKeys("left", "h"), Binding.withHelp("←/h", "move left"));
        this.right = new Binding(Binding.withKeys("right", "l"), Binding.withHelp("→/l", "move right"));
    }

    /**
     * Returns the binding for paging down.
     *
     * @return page-down binding
     */
    public Binding pageDown() {
        return pageDown;
    }

    /**
     * Returns the binding for paging up.
     *
     * @return page-up binding
     */
    public Binding pageUp() {
        return pageUp;
    }

    /**
     * Returns the binding for half-page down.
     *
     * @return half-page-down binding
     */
    public Binding halfPageDown() {
        return halfPageDown;
    }

    /**
     * Returns the binding for half-page up.
     *
     * @return half-page-up binding
     */
    public Binding halfPageUp() {
        return halfPageUp;
    }

    /**
     * Returns the binding for line-down movement.
     *
     * @return down binding
     */
    public Binding down() {
        return down;
    }

    /**
     * Returns the binding for line-up movement.
     *
     * @return up binding
     */
    public Binding up() {
        return up;
    }

    /**
     * Returns the binding for left movement.
     *
     * @return left binding
     */
    public Binding left() {
        return left;
    }

    /**
     * Returns the binding for right movement.
     *
     * @return right binding
     */
    public Binding right() {
        return right;
    }
}
