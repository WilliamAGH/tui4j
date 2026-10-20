package com.williamcallahan.tui4j.compat.bubbles.textinput;


import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests text input.
 * Bubble Tea: bubbletea/examples/textinput/main.go
 */
class TextInputTest {

    @BeforeEach
    void setUp() {
        // Set up no-color terminal info for consistent output
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        // Explicitly set ASCII color profile to avoid pollution from other tests
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    public void testCurrentSuggestion() {
        TextInput textInput = new TextInput();
        textInput.setShowSuggestions(true);

        // Initial state: no current suggestion
        String suggestion = textInput.currentSuggestion();
        String expected = "";
        assertEquals(expected, suggestion, "Expected no current suggestion initially");

        // Set suggestions but no value, so no suggestions should match
        textInput.setSuggestions(new String[]{"test1", "test2", "test3"});
        suggestion = textInput.currentSuggestion();
        expected = "";
        assertEquals(expected, suggestion, "Expected no current suggestion after setting suggestions without value");

        // Set a value, update suggestions, and navigate to the next suggestion
        textInput.setValue("test");
        textInput.updateSuggestions();
        textInput.nextSuggestion();
        suggestion = textInput.currentSuggestion();
        expected = "test2";
        assertEquals(expected, suggestion, "Expected first suggestion to be 'test2'");
    }

    @Test
    public void testSlicingOutsideCap() {
        TextInput textInput = new TextInput();
        textInput.setPlaceholder("作業ディレクトリを指定してください");
        textInput.setWidth(32);

        // Rendering the view should not throw an exception
        textInput.view();
    }

    /**
     * Verifies a bracketed-paste key inserts its text, the way upstream textinput does through
     * its default branch ({@code insertRunesFromUserInput(msg.Runes)}).
     * <p>
     * Upstream: github.com/charmbracelet/bubbles/textinput/textinput.go Update default branch.
     */
    @Test
    public void testPasteKeyInsertsPastedText() {
        TextInput textInput = new TextInput();
        textInput.focus();

        textInput.update(new KeyPressMessage(
            new Key(KeyType.KeyRunes, "hi there".toCharArray(), false, true)));

        assertEquals("hi there", textInput.value());
    }
}
