package com.williamcallahan.tui4j.compat.bubbles.help;

import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.williamcallahan.tui4j.compat.bubbles.key.Binding.withHelp;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests help.
 * Bubble Tea: bubbletea/examples/help/main.go
 */
class HelpTest {

    @BeforeEach
    void setUp() {
        // Set up no-color terminal info for consistent output
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        // Explicitly set ASCII color profile to avoid pollution from other tests
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @ParameterizedTest
    @MethodSource("provideWidthsAndExpectedOutputs")
    void testFullHelp(int width, String expected) {
        // Act
        String result = renderFullHelp(width);

        // Assert
        assertEquals(expected, result);
    }

    /**
     * Proves the help view keeps overflowing columns when the ellipsis itself cannot fit.
     * <p>
     * Upstream {@code bubbles/help/help.go shouldAddItem} only shortens the view when it
     * has a tail to print; when an item overflows and the ellipsis does not fit, it falls
     * through to {@code ("", true)} so the item is rendered and the following items keep
     * rendering too. Verified against bubbles v0.21.0, whose {@code FullHelpView} returns
     * the complete three-column block for widths 1 and 14 in this fixture.
     */
    @Test
    void testFullHelpKeepsOverflowingColumnsWhenEllipsisCannotFit() {
        String unlimited = renderFullHelp(40);

        assertEquals(unlimited, renderFullHelp(1));
        assertEquals(unlimited, renderFullHelp(14));
        assertEquals("enter continue …", renderFullHelp(20));
    }

    /**
     * Proves the short help view keeps overflowing bindings when the ellipsis cannot fit.
     * <p>
     * Verified against bubbles v0.21.0, whose {@code ShortHelpView} renders every binding
     * for widths 1 and 12 in this fixture.
     */
    @Test
    void testShortHelpKeepsOverflowingBindingsWhenEllipsisCannotFit() {
        String all = "↑/k move up • ↓/j move down • q quit";

        assertEquals(all, renderShortHelp(1));
        assertEquals(all, renderShortHelp(12));
        assertEquals("↑/k move up …", renderShortHelp(20));
    }

    private static String renderFullHelp(int width) {
        Help help = new Help();
        help.setFullSeparator(" | ");
        help.setWidth(width);
        help.setShowAll(true);
        return help.render(fullHelpFixture());
    }

    private static String renderShortHelp(int width) {
        Help help = new Help();
        help.setWidth(width);
        return help.render(shortHelpFixture());
    }

    private static KeyMap fullHelpFixture() {
        Binding.BindingOption k = Binding.withKeys("width");
        return new TestKeyMap(null, new Binding[][]{
                new Binding[]{new Binding(k, withHelp("enter", "continue"))},
                new Binding[]{
                        new Binding(k, withHelp("esc", "back")),
                        new Binding(k, withHelp("?", "help"))
                },
                new Binding[]{
                        new Binding(k, withHelp("H", "home")),
                        new Binding(k, withHelp("ctrl+c", "quit")),
                        new Binding(k, withHelp("ctrl+l", "log"))
                }
        });
    }

    private static KeyMap shortHelpFixture() {
        return new TestKeyMap(new Binding[]{
                new Binding(Binding.withKeys("up", "k"), withHelp("↑/k", "move up")),
                new Binding(Binding.withKeys("down", "j"), withHelp("↓/j", "move down")),
                new Binding(Binding.withKeys("q", "ctrl+c"), withHelp("q", "quit"))
        }, null);
    }

    private static class TestKeyMap implements KeyMap {
        private final Binding[] shortBindings;
        private final Binding[][] fullBindings;

        TestKeyMap(Binding[] shortBindings, Binding[][] fullBindings) {
            this.shortBindings = shortBindings;
            this.fullBindings = fullBindings;
        }

        @Override
        public Binding[][] fullHelp() {
            return fullBindings;
        }

        @Override
        public Binding[] shortHelp() {
            return shortBindings;
        }
    }

    private static Stream<Arguments> provideWidthsAndExpectedOutputs() {
        return Stream.of(
                Arguments.of(20, "enter continue …"),
                Arguments.of(30, String.join("\n",
                        "enter continue | esc back …",
                        "                 ?   help  "
                )),
                Arguments.of(40, String.join("\n",
                        "enter continue | esc back | H      home",
                        "                 ?   help   ctrl+c quit",
                        "                            ctrl+l log "
                ))
        );
    }
}
