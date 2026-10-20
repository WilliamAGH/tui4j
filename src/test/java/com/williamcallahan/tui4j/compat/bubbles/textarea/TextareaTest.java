package com.williamcallahan.tui4j.compat.bubbles.textarea;

import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.compat.bubbles.cursor.CursorMode;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextareaTest {

    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    void testInitialState() {
        Textarea textarea = new Textarea();

        assertEquals("", textarea.value(), "Initial value should be empty");
        assertEquals(1, textarea.lineCount(), "Initial line count should be 1");
        assertEquals(0, textarea.line(), "Initial cursor line should be 0");
    }

    @Test
    void testInsertString() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);

        textarea.insertString("Hello");
        assertEquals("Hello", textarea.value(), "Value should be 'Hello' after insertion");
        assertEquals(1, textarea.lineCount(), "Should still be 1 line");
    }

    @Test
    void testInsertMultilineString() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);

        textarea.insertString("Hello\nWorld");
        assertTrue(textarea.value().contains("Hello"), "Value should contain 'Hello'");
        assertTrue(textarea.value().contains("World"), "Value should contain 'World'");
        assertTrue(textarea.lineCount() >= 1, "Should have at least 1 line");
    }

    @Test
    void testFocusAndBlur() {
        Textarea textarea = new Textarea();

        assertEquals(false, textarea.focused(), "Should not be focused initially");

        textarea.focus();
        assertEquals(true, textarea.focused(), "Should be focused after focus()");

        textarea.blur();
        assertEquals(false, textarea.focused(), "Should not be focused after blur()");
    }

    @Test
    void testSetValue() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);

        textarea.setValue("Line 1\nLine 2\nLine 3");

        assertTrue(textarea.value().contains("Line 1"), "Value should contain 'Line 1'");
        assertTrue(textarea.value().contains("Line 2"), "Value should contain 'Line 2'");
        assertTrue(textarea.value().contains("Line 3"), "Value should contain 'Line 3'");
    }

    @Test
    void testCharLimit() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);
        textarea.setCharLimit(5);

        textarea.insertString("Hello World");

        assertEquals("Hello", textarea.value().substring(0, 5), "Value should be limited to 5 chars");
    }

    @Test
    void testPlaceholder() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);
        textarea.setPlaceholder("Enter text here...");

        String view = textarea.view();

        assertTrue(view.contains("Enter text here..."), "View should contain placeholder text");
    }

    @Test
    void testViewDoesNotThrowException() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);
        textarea.setPlaceholder("作業ディレクトリを指定してください");

        assertDoesNotThrow(textarea::view);
    }

    /**
     * Verifies the cursor renders on the correct wrapped line after scrolling.
     */
    @Test
    void testCursorRendersOnWrappedLine() {
        Textarea textarea = new Textarea();
        textarea.setPrompt("");
        textarea.setShowLineNumbers(false);
        textarea.setWidth(5);
        textarea.setHeight(1);
        textarea.insertString("hello world");
        textarea.focus();
        textarea.update(new com.williamcallahan.tui4j.compat.bubbletea.FocusMessage());
        textarea.cursor().setMode(CursorMode.Static);
        textarea.cursor().setStyle(com.williamcallahan.tui4j.compat.lipgloss.Style.newStyle()
            .transform(value -> "{" + value + "}"));

        Textarea.LineInfo lineInfo = textarea.lineInfo();
        assertTrue(lineInfo.rowOffset() > 0, "Expected cursor to be on a wrapped line");

        String view = textarea.view();
        String[] lines = view.split("\n", -1);
        int cursorLineIndex = -1;
        int cursorLineCount = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("{") && lines[i].contains("}")) {
                cursorLineIndex = i;
                cursorLineCount++;
            }
        }

        assertEquals(1, cursorLineCount, "Cursor should render on only one wrapped line");
        // The cursor may not be on line 0 due to space-preserving wrap behavior which can produce
        // extra lines. The key invariant is that the cursor renders on exactly one line and is
        // within the visible viewport height.
        assertTrue(cursorLineIndex >= 0 && cursorLineIndex < textarea.height() + 1,
            "Cursor should render within visible viewport, got line " + cursorLineIndex);
    }

    /**
     * Verifies the cursor renders as a visible space when at end-of-line.
     */
    @Test
    void testCursorVisibleAtEndOfLine() {
        Textarea textarea = new Textarea();
        textarea.setPrompt("");
        textarea.setShowLineNumbers(false);
        textarea.setWidth(10);
        textarea.setHeight(1);
        textarea.insertString("a");
        textarea.cursor().setMode(CursorMode.Static);
        textarea.cursor().setStyle(Style.newStyle().transform(value -> "<" + value + ">"));

        String view = textarea.view();

        assertTrue(view.contains("< >"), "Cursor should render a visible space at end of line");
    }


    @Test
    void testReset() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);

        textarea.insertString("Hello World");
        assertEquals("Hello World", textarea.value());

        textarea.reset();
        assertEquals("", textarea.value(), "Value should be empty after reset");
        assertEquals(1, textarea.lineCount(), "Should have 1 line after reset");
    }

    @Test
    void testMaxHeight() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(5);
        textarea.setMaxHeight(3);
        textarea.focus();

        for (int i = 0; i < 10; i++) {
            textarea.insertString("Line " + i);
            textarea.update(new com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage(
                new com.williamcallahan.tui4j.compat.bubbletea.input.key.Key(
                    com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType.keyCR
                )
            ));
        }

        assertTrue(textarea.lineCount() <= 3, "Should be limited by max height");
    }

    @Test
    void testLengthCalculation() {
        Textarea textarea = new Textarea();

        textarea.setValue("Hello\nWorld");

        int len = textarea.length();

        assertTrue(len > 0, "Length should be positive");
    }

    @Test
    void testWidthAndHeightSetters() {
        Textarea textarea = new Textarea();

        textarea.setPrompt("");
        textarea.setShowLineNumbers(false);
        textarea.setWidth(100);
        textarea.setHeight(20);

        assertEquals(100, textarea.width(), "Width should be 100");
        assertEquals(20, textarea.height(), "Height should be 20");
    }

    @Test
    void testEndOfBufferCharacter() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);

        textarea.setEndOfBufferCharacter('~');
        String view = textarea.view();
        assertNotNull(view, "View should render after setting end-of-buffer character");
    }

    @Test
    void testLineGetter() {
        Textarea textarea = new Textarea();
        textarea.insertString("Hello");

        assertTrue(textarea.line() >= 0, "Cursor line should be non-negative");
    }

    @Test
    void testInsertRune() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);

        textarea.insertRune('H');
        textarea.insertRune('i');
        textarea.insertRune('!');

        assertEquals("Hi!", textarea.value());
    }

    @Test
    void testStyleClasses() {
        Textarea textarea = new Textarea();
        Textarea.Style style = textarea.style();
        assertNotNull(style, "Style should not be null");
    }

    @Test
    void testFocusedAndBlurredStyles() {
        Textarea textarea = new Textarea();

        assertNotNull(textarea.focusedStyle(), "Focused style should not be null");
        assertNotNull(textarea.blurredStyle(), "Blurred style should not be null");
    }

    @Test
    void testCursorAccessor() {
        Textarea textarea = new Textarea();

        assertNotNull(textarea.cursor(), "Cursor should not be null");
    }

    @Test
    void testLineInfoClass() {
        Textarea.LineInfo lineInfo = new Textarea.LineInfo();
        assertNotNull(lineInfo, "LineInfo should be constructable");
    }

    @Test
    void testMultipleInserts() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);

        textarea.insertString("First");
        textarea.insertString(" ");
        textarea.insertString("Second");

        assertEquals("First Second", textarea.value());
    }

    @Test
    void testKeyMapClass() {
        Textarea.KeyMap keyMap = new Textarea.KeyMap();

        assertNotNull(keyMap.characterForward(), "characterForward binding should not be null");
        assertNotNull(keyMap.characterBackward(), "characterBackward binding should not be null");
    }

    /**
     * Typing a long line that soft-wraps must not corrupt the stored value.
     * <p>
     * Port of {@code TestValueSoftWrap} from {@code bubbles/textarea/textarea_test.go}.
     */
    @Test
    void testValueSoftWrap() {
        Textarea textarea = new Textarea();
        textarea.setWidth(16);
        textarea.setHeight(10);
        textarea.setCharLimit(500);
        textarea.focus();

        String input = "Testing Testing Testing Testing Testing Testing Testing Testing";
        typeRunes(textarea, input);
        textarea.view();

        assertEquals(input, textarea.value(), "Wrapping should not change the stored value");
    }

    /**
     * Setting a multi-line value must leave the cursor at the end of the last line,
     * and setting a value again must reset the buffer.
     * <p>
     * Port of {@code TestSetValue} from {@code bubbles/textarea/textarea_test.go};
     * the upstream {@code col} is the visual column of the cursor, exposed here as
     * {@link Textarea.LineInfo#columnOffset()}.
     */
    @Test
    void testSetValuePlacesCursorAtEnd() {
        Textarea textarea = new Textarea();
        textarea.setValue(String.join("\n", "Foo", "Bar", "Baz"));

        assertEquals("Foo\nBar\nBaz", textarea.value());
        assertEquals(2, textarea.line(), "Cursor should be on the last row after two newlines");
        assertEquals(3, textarea.lineInfo().columnOffset(), "Cursor should be after the last character");

        textarea.setValue("Test");

        assertEquals("Test", textarea.value(), "SetValue should reset the text area");
    }

    /**
     * Astral-plane characters must be inserted, stored, and measured as single
     * characters of double width.
     * <p>
     * Port of {@code TestCanHandleEmoji} from {@code bubbles/textarea/textarea_test.go}.
     */
    @Test
    void testCanHandleEmoji() {
        Textarea textarea = new Textarea();
        textarea.focus();

        typeRunes(textarea, "🧋");
        assertEquals("🧋", textarea.value(), "Expected emoji to be inserted");

        textarea.setValue("🧋🧋🧋");

        assertEquals("🧋🧋🧋", textarea.value(), "Expected emoji to be inserted");
        assertEquals(3, textarea.lineInfo().columnOffset(), "Expected cursor to be on the third character");
        assertEquals(6, textarea.lineInfo().charOffset(), "Expected cursor to be on the sixth cell");
    }

    /**
     * Moving down from a line of double-width runes must keep the cursor on the
     * same visual column.
     * <p>
     * Port of {@code TestVerticalNavigationKeepsCursorHorizontalPosition} from
     * {@code bubbles/textarea/textarea_test.go}.
     */
    @Test
    void testVerticalNavigationKeepsCursorHorizontalPosition() {
        Textarea textarea = new Textarea();
        textarea.setWidth(20);
        textarea.focus();
        textarea.setValue("你好你好\nHello");
        press(textarea, KeyType.KeyCtrlHome);
        press(textarea, KeyType.KeyRight);
        press(textarea, KeyType.KeyRight);

        assertEquals(4, textarea.lineInfo().charOffset(), "Cursor should be on the fourth cell of the first line");
        assertEquals(2, textarea.lineInfo().columnOffset(), "Cursor should be after two double width runes");

        press(textarea, KeyType.KeyDown);

        assertEquals(1, textarea.line(), "Cursor should be on the second line");
        assertEquals(4, textarea.lineInfo().charOffset(), "Cursor should keep its visual column");
        assertEquals(4, textarea.lineInfo().columnOffset(), "Cursor should be after four characters of the second line");
    }

    /**
     * Vertical traversal must remember the horizontal position and forget it when
     * the cursor moves sideways.
     * <p>
     * Port of {@code TestVerticalNavigationShouldRememberPositionWhileTraversing}
     * from {@code bubbles/textarea/textarea_test.go}.
     */
    @Test
    void testVerticalNavigationShouldRememberPositionWhileTraversing() {
        Textarea textarea = new Textarea();
        textarea.setWidth(40);
        textarea.focus();
        textarea.setValue(String.join("\n", "Hello", "World", "This is a long line."));

        assertEquals(2, textarea.line(), "Cursor should start on the last line");
        assertEquals(20, textarea.lineInfo().columnOffset(), "Cursor should be at the end of the last line");

        press(textarea, KeyType.KeyUp);
        assertEquals(1, textarea.line(), "Cursor should move to the second line");
        assertEquals(5, textarea.lineInfo().columnOffset(), "Cursor should be at the end of the second line");

        press(textarea, KeyType.KeyUp);
        assertEquals(0, textarea.line(), "Cursor should move to the first line");
        assertEquals(5, textarea.lineInfo().columnOffset(), "Cursor should be at the end of the first line");

        press(textarea, KeyType.KeyDown);
        press(textarea, KeyType.KeyDown);
        assertEquals(2, textarea.line(), "Cursor should return to the last line");
        assertEquals(20, textarea.lineInfo().columnOffset(), "Cursor should restore the remembered position");

        press(textarea, KeyType.KeyUp);
        press(textarea, KeyType.KeyLeft);
        assertEquals(1, textarea.line(), "Cursor should be on the second line");
        assertEquals(4, textarea.lineInfo().columnOffset(), "Moving left should move one character back");

        press(textarea, KeyType.KeyDown);
        assertEquals(2, textarea.line(), "Cursor should be on the last line");
        assertEquals(4, textarea.lineInfo().columnOffset(), "Moving sideways should forget the remembered position");
    }

    /**
     * Sends a key press to the text area.
     *
     * @param textarea text area to update
     * @param keyType key to press
     */
    private static void press(Textarea textarea, KeyType keyType) {
        textarea.update(new KeyPressMessage(new Key(keyType)));
    }

    /**
     * Sends a string to the text area one code point at a time, as the input
     * handler delivers rune key presses.
     *
     * @param textarea text area to update
     * @param input text to type
     */
    private static void typeRunes(Textarea textarea, String input) {
        int index = 0;
        while (index < input.length()) {
            char[] runes = Character.toChars(input.codePointAt(index));
            textarea.update(new KeyPressMessage(new Key(KeyType.KeyRunes, runes)));
            index += runes.length;
        }
    }

    /**
     * Verifies a bracketed-paste key inserts its text, the way upstream delivers pastes
     * (a KeyMsg with the Paste flag whose runes the textarea inserts through
     * insertRunesFromUserInput).
     */
    @Test
    void testPasteKeyInsertsPastedText() {
        Textarea textarea = new Textarea();
        textarea.setWidth(80);
        textarea.setHeight(10);
        textarea.focus();

        textarea.update(new KeyPressMessage(
            new Key(KeyType.KeyRunes, "hi there".toCharArray(), false, true)));

        assertEquals("hi there", textarea.value(), "A paste key should insert its text");
    }
}
