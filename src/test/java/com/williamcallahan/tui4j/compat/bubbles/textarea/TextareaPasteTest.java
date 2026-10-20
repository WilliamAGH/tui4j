package com.williamcallahan.tui4j.compat.bubbles.textarea;

import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;
import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the clipboard paste reply the textarea consumes.
 * <p>
 * Upstream: github.com/charmbracelet/bubbles/textarea/textarea.go - {@code Paste} reads the
 * clipboard and {@code case pasteMsg: m.insertRunesFromUserInput([]rune(msg))} inserts it; the
 * port's equivalent reply is {@link PasteMessage}, produced by
 * {@link com.williamcallahan.tui4j.compat.bubbletea.Command#paste()}.
 */
class TextareaPasteTest {

    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    @DisplayName("a clipboard paste reply inserts its text")
    void test_PasteMessageInsertsClipboardText() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);
        textarea.focus();

        textarea.update(new PasteMessage("hi there"));

        assertEquals("hi there", textarea.value(),
            "The clipboard paste reply should be inserted at the cursor");
    }
}
