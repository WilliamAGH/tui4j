package com.williamcallahan.tui4j.compat.bubbletea.render;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies printed lines wider than the terminal count every row they wrap onto, so the
 * normal-screen erase below a shrinking view never wipes the view at the bottom margin.
 */
class RendererQueuedWrapTest {

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
