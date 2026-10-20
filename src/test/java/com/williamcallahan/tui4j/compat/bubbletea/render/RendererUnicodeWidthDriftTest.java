package com.williamcallahan.tui4j.compat.bubbletea.render;

import com.williamcallahan.tui4j.compat.x.ansi.StringWidth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that terminal-specific Unicode width does not shift alternate-screen rows.
 */
class RendererUnicodeWidthDriftTest {

    /**
     * Keeps changing status and footer rows in place when a terminal expands a
     * joined emoji into the widths of its component code points. The emoji row
     * itself changes each frame, so the second flush repaints it while the row
     * beneath it stays diff-skipped: width drift that escaped the emoji row would
     * corrupt a physical row that is never rewritten.
     */
    @Test
    void joinedEmojiCannotDisplaceNeighboringRows() {
        int width = 24;
        int height = 8;
        RendererTestTerminal terminal = new RendererTestTerminal(width, height);
        RendererFlush flush = new RendererFlush(terminal.terminal());
        TerminalCellModel screen = new TerminalCellModel(width, height);
        flush.enterAltScreen();
        terminal.drain();

        for (int frame = 0; frame < 2; frame++) {
            String view = String.join("\n",
                pad("family " + frame + " 👨‍👩‍👧‍👦 transcript", width),
                pad("second transcript", width),
                pad("Working " + frame, width),
                " ".repeat(width),
                "─".repeat(width),
                pad("/project/path", width),
                pad("stats " + frame, width),
                pad("footer", width)
            );
            flush.write(view, true);
            flush.flush(width, height);
            screen.apply(terminal.drain());
        }

        assertThat(screen.line(0)).startsWith("family 1");
        assertThat(screen.line(1)).startsWith("second transcript");
        assertThat(screen.line(2)).startsWith("Working 1");
        assertThat(screen.line(3)).isBlank();
        assertThat(screen.line(4)).isEqualTo("─".repeat(width));
        assertThat(screen.line(6)).startsWith("stats 1");
    }

    /**
     * Pads a logical row using the renderer's canonical grapheme-width contract.
     *
     * @param value row content
     * @param width target logical width
     * @return full-width logical row
     */
    private static String pad(String value, int width) {
        return value + " ".repeat(width - StringWidth.stringWidth(value));
    }
}
