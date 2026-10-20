package com.williamcallahan.tui4j.compat.bubbles.viewport;

import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the default viewport key bindings against the mapped upstream
 * {@code viewport/keymap.go} ({@code DefaultKeyMap}).
 */
class KeyMapTest {

    private final KeyMap keyMap = new KeyMap();

    @Test
    void testPageDownKeys() {
        assertThat(matches(keyMap.pageDown(), 'f')).isTrue();
        assertThat(matches(keyMap.pageDown(), KeyType.KeySpace)).isTrue();
        assertThat(matches(keyMap.pageDown(), KeyType.KeyPgDown)).isTrue();
    }

    @Test
    void testPageUpKeys() {
        assertThat(matches(keyMap.pageUp(), 'b')).isTrue();
        assertThat(matches(keyMap.pageUp(), KeyType.KeyPgUp)).isTrue();
    }

    @Test
    void testHalfPageKeys() {
        assertThat(matches(keyMap.halfPageDown(), 'd')).isTrue();
        assertThat(matches(keyMap.halfPageDown(), KeyType.keyEOT)).as("ctrl+d").isTrue();
        assertThat(matches(keyMap.halfPageUp(), 'u')).isTrue();
        assertThat(matches(keyMap.halfPageUp(), KeyType.keyNAK)).as("ctrl+u").isTrue();
    }

    @Test
    void testLineKeys() {
        assertThat(matches(keyMap.down(), 'j')).isTrue();
        assertThat(matches(keyMap.down(), KeyType.KeyDown)).isTrue();
        assertThat(matches(keyMap.up(), 'k')).isTrue();
        assertThat(matches(keyMap.up(), KeyType.KeyUp)).isTrue();
    }

    @Test
    void testHorizontalKeys() {
        assertThat(matches(keyMap.left(), 'h')).isTrue();
        assertThat(matches(keyMap.left(), KeyType.KeyLeft)).isTrue();
        assertThat(matches(keyMap.right(), 'l')).isTrue();
        assertThat(matches(keyMap.right(), KeyType.KeyRight)).isTrue();
    }

    /**
     * The pager-style control keys upstream reserves for paging must not
     * trigger the half-page or line bindings.
     */
    @Test
    void testPagingKeysStayOutOfOtherBindings() {
        assertThat(matches(keyMap.halfPageDown(), 'f')).as("f is page down").isFalse();
        assertThat(matches(keyMap.halfPageUp(), 'b')).as("b is page up").isFalse();
        assertThat(matches(keyMap.pageDown(), KeyType.keyEOT)).as("ctrl+d is half page down").isFalse();
        assertThat(matches(keyMap.pageUp(), KeyType.keyNAK)).as("ctrl+u is half page up").isFalse();
    }

    private static boolean matches(Binding binding, char rune) {
        return binding.matches(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[]{rune})));
    }

    private static boolean matches(Binding binding, KeyType type) {
        return binding.matches(new KeyPressMessage(new Key(type)));
    }
}
