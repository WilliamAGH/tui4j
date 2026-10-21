package com.williamcallahan.tui4j.term;

import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link TerminalInfo} when no provider has ever been installed.
 * <p>
 * Oracle: upstream lipgloss renders with zero initialization - against piped (non-TTY) stdout,
 * {@code lipgloss.NewStyle().Foreground(...).Background(...).Render("hello")} returns
 * {@code "hello"} with no escape sequences and no failure, so "no terminal detected" means
 * not-a-TTY and no colors. Every provisioning site in this repository (the twelve test classes
 * and {@code ProgramTerminal}) installs {@code new TerminalInfo(false, new NoColor())}, which is
 * the established default this contract pins.
 */
class TerminalInfoTest {

    @AfterEach
    void restoreTerminalInfo() {
        // Leave the shared provider in the state the other test classes install.
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
    }

    @Test
    void testGetWithoutAProviderReturnsTheNonTtyDefault() {
        // given - no provider installed
        TerminalInfo.provide(null);

        // when
        TerminalInfo terminalInfo = TerminalInfo.get();

        // then
        assertThat(terminalInfo.tty()).isFalse();
        assertThat(terminalInfo.backgroundColor()).isInstanceOf(NoColor.class);
    }

    @Test
    void testRenderWithoutAProviderProducesPlainOutput() {
        // given - no provider installed
        TerminalInfo.provide(null);

        // when
        String rendered = Style.newStyle().render("Foo");

        // then
        assertThat(rendered).isEqualTo("Foo");
    }
}
