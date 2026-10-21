package com.williamcallahan.tui4j.compat.bubbletea;

import com.williamcallahan.tui4j.term.Clipboard;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents a command that yields a Message.
 * <p>
 * Port of github.com/charmbracelet/bubbletea/commands.go.
 */
public interface Command {

    /** Sentinel command representing "no command to execute" (Go's nil Cmd). */
    Command NO_OP = () -> null;

    /**
     * Executes the command and returns a message.
     *
     * @return message to deliver
     */
    Message execute();

    /**
     * Returns a no-op command sentinel, equivalent to Go's nil Cmd.
     * <p>
     * Use this instead of returning {@code null} from methods whose return
     * type is {@link Command}.
     *
     * @return no-op command
     */
    static Command none() {
        return NO_OP;
    }

    /**
     * Checks whether a command is absent (null or the no-op sentinel).
     *
     * @param command command to check
     * @return true if the command should not be executed
     */
    static boolean isNone(Command command) {
        return command == null || command == NO_OP;
    }

    /**
     * Batches commands into a single message.
     *
     * @param commands commands to batch
     * @return batch command
     */
    static Command batch(Collection<Command> commands) {
        Command[] filteredCommands = commands
            .stream()
            .filter(c -> !isNone(c))
            .toArray(Command[]::new);
        return () -> new BatchMessage(filteredCommands);
    }

    /**
     * Batches commands into a single message.
     *
     * @param commands commands to batch
     * @return batch command
     */
    static Command batch(Command... commands) {
        Command[] filteredCommands = Arrays.stream(commands)
            .filter(c -> !isNone(c))
            .toArray(Command[]::new);
        return () -> new BatchMessage(filteredCommands);
    }

    /**
     * Runs commands sequentially.
     *
     * @param commands commands to run
     * @return sequence command
     */
    static Command sequence(Command... commands) {
        return () -> new SequenceMessage(commands);
    }

    /**
     * Runs commands in order and returns the first non-null message.
     *
     * @param commands commands to execute
     * @return sequential command
     * @see <a href="https://github.com/charmbracelet/bubbletea/blob/main/commands.go">bubbletea/commands.go</a>
     */
    static Command sequentially(Command... commands) {
        return () -> {
            if (commands == null) {
                return null;
            }
            for (Command command : commands) {
                if (command == null) {
                    continue;
                }
                Message message = command.execute();
                if (message != null) {
                    return message;
                }
            }
            return null;
        };
    }

    /**
     * Emits a message after a duration.
     *
     * @param duration delay duration
     * @param fn       function to map time to message
     * @return tick command
     */
    static Command tick(Duration duration, Function<LocalDateTime, Message> fn) {
        return CommandTimer.tick(duration, fn);
    }

    /**
     * Emits a message aligned to the system clock.
     *
     * @param duration tick duration
     * @param fn function to map time to message
     * @return every command
     */
    static Command every(Duration duration, Function<LocalDateTime, Message> fn) {
        return CommandTimer.every(duration, fn);
    }

    /**
     * Emits a line with the provided arguments.
     * <p>
     * The body is joined the way Go's {@code fmt.Sprint} joins {@code Println} arguments:
     * operands keep their string form and a space separates two operands only when neither
     * operand is a string.
     * Bubble Tea: standard_renderer.go Println.
     *
     * @param arguments values to join
     * @return print line command
     */
    static Command println(Object... arguments) {
        return () -> new PrintLineMessage(formatSprint(arguments));
    }

    /**
     * Concatenates operands with Go's {@code fmt.Sprint} spacing rule.
     * <p>
     * Bubble Tea: standard_renderer.go Println.
     *
     * @param arguments operands to concatenate
     * @return concatenated text
     */
    private static String formatSprint(Object... arguments) {
        StringBuilder body = new StringBuilder();
        boolean previousWasString = false;
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            boolean isString = argument instanceof String;
            if (i > 0 && !isString && !previousWasString) {
                body.append(' ');
            }
            previousWasString = isString;
            body.append(String.valueOf(argument));
        }
        return body.toString();
    }

    /**
     * Emits a formatted line.
     *
     * @param template  format string
     * @param arguments format arguments
     * @return print line command
     */
    static Command printf(String template, Object... arguments) {
        return () -> new PrintLineMessage(template.formatted(arguments));
    }

    /**
     * Sets the terminal window title.
     *
     * @param title the title to set
     * @return the command
     */
    static Command setWindowTitle(String title) {
        return () -> new SetWindowTitleMessage(title);
    }

    /**
     * Sets the terminal window title.
     * <p>
     * Kept as a spelling-compatible alias for {@link #setWindowTitle(String)}.
     *
     * @param title the title to set
     * @return the command
     */
    static Command setWidowTitle(String title) {
        return setWindowTitle(title);
    }

    /**
     * Clears the terminal screen.
     *
     * @return clear screen command
     */
    static Command clearScreen() {
        return ClearScreenMessage::new;
    }

    /**
     * Requests a window size check.
     *
     * @return check window size command
     */
    static Command checkWindowSize() {
        return com.williamcallahan.tui4j.message.CheckWindowSizeMessage::new;
    }

    /**
     * Quits the program.
     *
     * @return quit command
     */
    static Command quit() {
        return QuitMessage::new;
    }

    /**
     * Sets the mouse cursor to text mode.
     *
     * @return set cursor text command
     */
    static Command setMouseCursorText() {
        return com.williamcallahan.tui4j.message.SetMouseCursorTextMessage::new;
    }

    /**
     * Sets the mouse cursor to pointer mode.
     *
     * @return set cursor pointer command
     */
    static Command setMouseCursorPointer() {
        return com.williamcallahan.tui4j.message.SetMouseCursorPointerMessage::new;
    }

    /**
     * Resets the mouse cursor to the default.
     *
     * @return reset cursor command
     */
    static Command resetMouseCursor() {
        return com.williamcallahan.tui4j.message.ResetMouseCursorMessage::new;
    }

    /**
     * Enables mouse cell motion reporting.
     *
     * @return enable mouse cell motion command
     */
    static Command enableMouseCellMotion() {
        return EnableMouseCellMotionMessage::new;
    }

    /**
     * Enables mouse all-motion reporting.
     *
     * @return enable mouse all motion command
     */
    static Command enableMouseAllMotion() {
        return EnableMouseAllMotionMessage::new;
    }

    /**
     * Disables mouse reporting.
     *
     * @return disable mouse command
     */
    static Command disableMouse() {
        return DisableMouseMessage::new;
    }

    /**
     * Copies text to the clipboard.
     *
     * @param text text to copy
     * @return clipboard command
     */
    static Command copyToClipboard(String text) {
        return () -> new com.williamcallahan.tui4j.message.CopyToClipboardMessage(text);
    }

    /**
     * Reads the system clipboard and delivers its contents as a {@link PasteMessage}.
     * <p>
     * Yields {@code null} when the clipboard is unavailable or reading is disabled with
     * {@code tui4j.clipboard.disabled}. Mirrors upstream bubbles, whose paste commands read the
     * local clipboard inside the command and hand the text back as a paste message
     * ({@code textinput.Paste}/{@code textarea.Paste}).
     *
     * @return paste command
     */
    static Command paste() {
        return () -> {
            String content = Clipboard.tryPaste();
            return content == null ? null : new PasteMessage(content);
        };
    }

    /**
     * Enables bracketed paste mode in the terminal.
     * <p>
     * Bubble Tea: bubbletea/commands.go EnableBracketedPaste
     *
     * @return enable bracketed paste command
     */
    static Command enableBracketedPaste() {
        return EnableBracketedPasteMessage::new;
    }

    /**
     * Disables bracketed paste mode in the terminal.
     * <p>
     * Bubble Tea: bubbletea/commands.go DisableBracketedPaste
     *
     * @return disable bracketed paste command
     */
    static Command disableBracketedPaste() {
        return DisableBracketedPasteMessage::new;
    }

    /**
     * Opens a URL via the host OS.
     *
     * @param url URL to open
     * @return open URL command
     */
    static Command openUrl(String url) {
        return () -> new com.williamcallahan.tui4j.message.OpenUrlMessage(url);
    }

    /**
     * Executes a process with output handlers.
     *
     * @param process       process to execute
     * @param outputHandler output handler
     * @param errorHandler  error handler
     * @return exec command
     */
    static Command execProcess(
        Process process,
        BiConsumer<Integer, byte[]> outputHandler,
        BiConsumer<Integer, byte[]> errorHandler
    ) {
        return () ->
            new ExecProcessMessage(process, outputHandler, errorHandler);
    }

    /**
     * Executes a process without output handlers.
     *
     * @param process process to execute
     * @return exec command
     */
    static Command execProcess(Process process) {
        return () -> new ExecProcessMessage(process, null, null);
    }
}
