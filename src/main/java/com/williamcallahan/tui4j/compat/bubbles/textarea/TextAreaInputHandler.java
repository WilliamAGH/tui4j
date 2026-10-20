package com.williamcallahan.tui4j.compat.bubbles.textarea;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbles.cursor.Cursor;
import com.williamcallahan.tui4j.compat.bubbles.cursor.CursorMode;
import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.bubbles.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

import static com.williamcallahan.tui4j.compat.bubbletea.Command.batch;

/**
 * Handles textarea input updates.
 * <p>
 * Port of {@code bubbles/textarea/textarea.go} update logic.
 */
final class TextAreaInputHandler {

    private final TextAreaBuffer buffer;
    private final TextAreaNavigation navigation;
    private final TextAreaLayout layout;

    /**
     * Creates an input handler.
     *
     * @param buffer buffer mutation helper
     * @param navigation navigation helper
     * @param layout layout helper
     */
    TextAreaInputHandler(TextAreaBuffer buffer, TextAreaNavigation navigation, TextAreaLayout layout) {
        this.buffer = buffer;
        this.navigation = navigation;
        this.layout = layout;
    }

    /**
     * Updates the textarea state based on the message.
     *
     * @param state textarea state
     * @param msg incoming message
     * @return update command
     */
    Command update(TextAreaState state, Message msg) {
        if (!state.focus) {
            state.cursor.blur();
            return null;
        }

        int oldRow = layout.cursorLineNumber(state);
        int oldCol = state.col;

        if (state.value.get(state.row) == null) {
            state.value.set(state.row, new int[0]);
        }

        if (state.maxHeight > 0 && state.maxHeight != state.wrapCache.capacity()) {
            state.wrapCache.reset(state.maxHeight);
        }

        List<Command> commands = new ArrayList<>();

        if (msg instanceof KeyPressMessage keyPressMessage) {
            if (Binding.matches(keyPressMessage, state.keyMap.deleteAfterCursor())) {
                state.col = clamp(state.col, 0, state.value.get(state.row).length);
                if (state.col >= state.value.get(state.row).length) {
                    buffer.mergeLineBelow(state, state.row);
                } else {
                    buffer.deleteAfterCursor(state);
                }
            } else if (Binding.matches(keyPressMessage, state.keyMap.deleteBeforeCursor())) {
                state.col = clamp(state.col, 0, state.value.get(state.row).length);
                if (state.col <= 0) {
                    buffer.mergeLineAbove(state, state.row);
                } else {
                    buffer.deleteBeforeCursor(state);
                }
            } else if (Binding.matches(keyPressMessage, state.keyMap.deleteCharacterBackward())) {
                buffer.deleteCharacterBackward(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.deleteCharacterForward())) {
                buffer.deleteCharacterForward(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.deleteWordBackward())) {
                if (state.col <= 0) {
                    buffer.mergeLineAbove(state, state.row);
                } else {
                    buffer.deleteWordLeft(state);
                }
            } else if (Binding.matches(keyPressMessage, state.keyMap.deleteWordForward())) {
                state.col = clamp(state.col, 0, state.value.get(state.row).length);
                if (state.col >= state.value.get(state.row).length) {
                    buffer.mergeLineBelow(state, state.row);
                } else {
                    buffer.deleteWordRight(state);
                }
            } else if (Binding.matches(keyPressMessage, state.keyMap.insertNewline())) {
                if (state.maxHeight > 0 && state.value.size() >= state.maxHeight) {
                    return null;
                }
                state.col = clamp(state.col, 0, state.value.get(state.row).length);
                buffer.splitLine(state, state.row, state.col);
            } else if (Binding.matches(keyPressMessage, state.keyMap.lineEnd())) {
                navigation.cursorEnd(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.lineStart())) {
                navigation.cursorStart(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.characterForward())) {
                navigation.characterRight(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.lineNext())) {
                navigation.cursorDown(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.wordForward())) {
                navigation.wordRight(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.paste())) {
                commands.add(Command.paste());
            } else if (Binding.matches(keyPressMessage, state.keyMap.characterBackward())) {
                navigation.characterLeft(state, false);
            } else if (Binding.matches(keyPressMessage, state.keyMap.linePrevious())) {
                navigation.cursorUp(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.wordBackward())) {
                navigation.wordLeft(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.inputBegin())) {
                navigation.moveToBegin(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.inputEnd())) {
                navigation.moveToEnd(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.lowercaseWordForward())) {
                navigation.lowercaseRight(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.uppercaseWordForward())) {
                navigation.uppercaseRight(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.capitalizeWordForward())) {
                navigation.capitalizeRight(state);
            } else if (Binding.matches(keyPressMessage, state.keyMap.transposeCharacterBackward())) {
                buffer.transposeLeft(state);
            } else {
                buffer.insertRunesFromUserInput(state, TextAreaRunes.toCodePoints(keyPressMessage.runes()));
            }
        } else if (msg instanceof PasteMessage pasteMessage) {
            buffer.insertRunesFromUserInput(state, TextAreaRunes.toCodePoints(pasteMessage.content()));
        }

        // Upstream textarea.New clears the viewport key map (vp.KeyMap = viewport.KeyMap{}),
        // so typed pager keys such as d, f, j and space never scroll the input; only
        // non-key messages such as the mouse wheel reach the viewport.
        if (!(msg instanceof KeyPressMessage)) {
            UpdateResult<Viewport> viewportUpdate = state.viewport.update(msg);
            state.viewport = viewportUpdate.model();
            commands.add(viewportUpdate.command());
        }

        int newRow = layout.cursorLineNumber(state);
        int newCol = state.col;

        UpdateResult<Cursor> cursorUpdate = state.cursor.update(msg);
        state.cursor = cursorUpdate.model();
        commands.add(cursorUpdate.command());

        if ((newRow != oldRow || newCol != oldCol) && state.cursor.mode() == CursorMode.Blink) {
            state.cursor.setBlink(false);
            commands.add(state.cursor.blinkCommand());
        }

        int minimum = state.viewport.getYOffset();
        int maximum = minimum + state.viewport.getHeight() - 1;
        if (newRow < minimum) {
            state.viewport.scrollUp(minimum - newRow);
        } else if (newRow > maximum) {
            state.viewport.scrollDown(newRow - maximum);
        }

        return batch(commands.toArray(new Command[0]));
    }

    /**
     * Clamps a value to the inclusive range.
     *
     * @param value value to clamp
     * @param low minimum value
     * @param high maximum value
     * @return clamped value
     */
    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }
}
