package com.williamcallahan.tui4j.term;

import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.compat.lipgloss.color.TerminalColor;

/**
 * Represents terminal capability and background info.
 * tui4j: src/main/java/com/williamcallahan/tui4j/term/TerminalInfo.java
 *
 * @param tty whether the terminal is a TTY
 * @param backgroundColor terminal background color
 */
public record TerminalInfo(boolean tty, TerminalColor backgroundColor) {

    /**
     * Terminal info used while no provider is installed: not a TTY and no background color.
     * <p>
     * Upstream lipgloss renders plain output until a terminal is detected (piped, non-TTY
     * stdout yields no color), so "no provider yet" must behave like "no terminal" instead of
     * failing every style render that runs before a {@code Program} starts.
     */
    private static final TerminalInfo NON_TTY = new TerminalInfo(false, new NoColor());

    private static TerminalInfoProvider infoProvider;

    /**
     * Sets the terminal info provider.
     *
     * @param infoProvider provider to use
     */
    public static void provide(TerminalInfoProvider infoProvider) {
        TerminalInfo.infoProvider = infoProvider;
    }

    /**
     * Returns terminal info from the configured provider, or the non-TTY default
     * {@code (false, NoColor)} while no provider has been installed.
     *
     * @return terminal info
     */
    public static TerminalInfo get() {
        if (infoProvider == null) {
            return NON_TTY;
        }
        return infoProvider.provide();
    }
}