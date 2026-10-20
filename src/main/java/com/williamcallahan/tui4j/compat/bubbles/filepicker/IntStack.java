package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import java.util.ArrayList;
import java.util.List;

/**
 * Integer stack backing the file picker's view history.
 * <p>
 * Port of the closure-based {@code stack} struct in
 * github.com/charmbracelet/bubbles/filepicker/filepicker.go.
 */
final class IntStack {

    private final List<Integer> items = new ArrayList<>();

    /**
     * Pushes an item onto the stack.
     *
     * @param item item to push
     */
    void push(int item) {
        this.items.add(item);
    }

    /**
     * Pops the top item.
     *
     * @return popped item or {@code 0} when empty
     */
    int pop() {
        if (this.items.isEmpty()) {
            return 0;
        }
        int result = this.items.get(this.items.size() - 1);
        this.items.remove(this.items.size() - 1);
        return result;
    }

    /**
     * Returns the number of items on the stack.
     *
     * @return stack size
     */
    int length() {
        return this.items.size();
    }
}
