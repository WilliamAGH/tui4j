package com.williamcallahan.tui4j.compat.bubbletea.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies printed lines wider than the terminal count every row they wrap onto, so the
 * normal-screen erase below a shrinking view never wipes the view at the bottom margin.
 */
class RendererQueuedWrapTest {

    /** Printed lines must replace the old view even before terminal width is known. */
    @ParameterizedTest
    @ValueSource(strings = {"X", "12345678901234"})
    void printedLineReplacesOldViewWhenWidthIsUnknown(String printed) {
        RendererTestTerminal terminal = new RendererTestTerminal(14, 6);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(14, 6);
        flush.write("123456789\nfooter", true);
        flush.flush(0, 6);
        screen.apply(terminal.drain());

        flush.queuePrintLine(printed);
        flush.write("body\nfooter", true);
        flush.flush(0, 6);
        screen.apply(terminal.drain());

        assertThat(screen.line(0)).isEqualTo(String.format("%-14s", printed));
        assertThat(screen.line(1)).startsWith("body");
        assertThat(screen.line(2)).startsWith("footer");
    }

    /** Unknown-width view repainting must also preserve an occupied right-margin cell. */
    @ParameterizedTest
    @ValueSource(strings = {"X", "12345678901234"})
    void viewLineReplacesOldViewWhenWidthIsUnknown(String view) {
        RendererTestTerminal terminal = new RendererTestTerminal(14, 6);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(14, 6);
        flush.write("previous view", true);
        flush.flush(0, 6);
        screen.apply(terminal.drain());

        flush.write(view, true);
        flush.flush(0, 6);
        screen.apply(terminal.drain());

        assertThat(screen.line(0)).isEqualTo(String.format("%-14s", view));
    }

    @Test
    void widePrintedLineAboveShorterViewKeepsTheViewRow() {
        RendererTestTerminal terminal = new RendererTestTerminal(14, 6);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(14, 6);
        screen.positionCursor(3, 0);
        flush.write("view 1\nview 2\nview 3", true);
        flush.flush(14, 6);
        screen.apply(terminal.drain());

        flush.queuePrintLine("printed line that wraps");
        flush.write("only", true);
        flush.flush(14, 6);
        screen.apply(terminal.drain());

        assertThat(screen.line(3)).startsWith("printed line t");
        assertThat(screen.line(4)).startsWith("hat wraps");
        assertThat(screen.line(5)).startsWith("only");
    }
}
