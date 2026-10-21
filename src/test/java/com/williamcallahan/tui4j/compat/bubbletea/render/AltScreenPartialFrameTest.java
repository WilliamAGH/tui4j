package com.williamcallahan.tui4j.compat.bubbletea.render;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies an alternate-screen repaint never shows a row erased ahead of its redraw.
 * <p>
 * Terminals read output in chunks and may paint between reads, so every prefix of a
 * frame is a state the user can see.
 */
class AltScreenPartialFrameTest {

    private static final int WIDTH = 20;
    private static final int HEIGHT = 4;
    private static final Pattern TERMINAL_UNIT = Pattern.compile("\u001b\\[[0-9;?]*[A-Za-z]|.", Pattern.DOTALL);

    @Test
    void everyPrefixOfARepaintKeepsEachBorderedRowsRightEdge() {
        RendererTestTerminal terminal = new RendererTestTerminal(WIDTH, HEIGHT);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(WIDTH, HEIGHT);
        flush.enterAltScreen();
        flush.write(borderedFrame("one"), true);
        flush.flush(WIDTH, HEIGHT);
        screen.apply(terminal.drain());

        flush.write(borderedFrame("two"), true);
        flush.flush(WIDTH, HEIGHT);
        Matcher unit = TERMINAL_UNIT.matcher(terminal.drain());
        while (unit.find()) {
            screen.apply(unit.group());
            for (int row = 0; row < HEIGHT; row++) {
                assertThat(screen.line(row)).as("row %d after %s", row, unit.group()).endsWith("│");
            }
        }

        assertThat(screen.line(HEIGHT - 1)).startsWith("│ two");
    }

    /**
     * Builds a full-width frame whose every row ends in a border cell.
     *
     * @param text row label
     * @return frame view
     */
    private static String borderedFrame(String text) {
        String row = "│ " + text;
        row += " ".repeat(WIDTH - 1 - row.length()) + "│";
        return String.join("\n", row, row, row, row);
    }
}
