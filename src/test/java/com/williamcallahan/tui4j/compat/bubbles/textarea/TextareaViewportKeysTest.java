package com.williamcallahan.tui4j.compat.bubbles.textarea;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that typed characters never drive the textarea's internal viewport.
 * <p>
 * Upstream bubbles/textarea/textarea.go {@code New} clears the viewport key map
 * ({@code vp.KeyMap = viewport.KeyMap{}}), so pager keys such as {@code d},
 * {@code f}, {@code j} and space only insert text.
 */
class TextareaViewportKeysTest {

    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    void typingPagerKeysOnTheSecondLineKeepsTheFirstLineVisible() {
        Textarea textarea = new Textarea();
        textarea.setPrompt("> ");
        textarea.setShowLineNumbers(false);
        textarea.setMaxHeight(6);
        textarea.setWidth(40);
        textarea.focus();

        type(textarea, "first");
        // A rendered frame loads the viewport content the pager keys would scroll.
        textarea.view();
        textarea.update(new KeyPressMessage(new Key(KeyType.keyCR)));
        textarea.view();
        type(textarea, "second dfj");

        String[] rows = textarea.view().split("\n");
        assertThat(rows[0]).contains("first");
        assertThat(rows[1]).contains("second dfj");
    }

    private static void type(Textarea textarea, String text) {
        for (char character : text.toCharArray()) {
            textarea.update(new KeyPressMessage(new Key(KeyType.KeyRunes, new char[] {character})));
        }
    }
}
