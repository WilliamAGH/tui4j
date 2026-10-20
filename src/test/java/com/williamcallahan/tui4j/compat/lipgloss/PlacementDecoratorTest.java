package com.williamcallahan.tui4j.compat.lipgloss;

import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the placement helpers with the upstream vertical layout rules.
 * <p>
 * Port of github.com/charmbracelet/lipgloss/position.go ({@code Place},
 * {@code PlaceHorizontal}, {@code PlaceVertical}); the expected values mirror the
 * upstream Go output, where a partially positioned block splits the remaining
 * gap into leading and trailing blank lines.
 */
class PlacementDecoratorTest {

    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("verticalPlacementData")
    void test_ShouldPlaceTextVertically(String name, int height, Position position, String input, String expected) {
        // when
        String placed = PlacementDecorator.placeVertical(height, position, input);

        // then
        assertThat(placed).isEqualTo(expected);
    }

    private static Stream<Arguments> verticalPlacementData() {
        return Stream.of(
                // Center splits the gap into equal halves (the blank rows are padded
                // to the width of the widest line, as upstream does).
                Arguments.of("center selects the middle rows", 5, Position.Center, "Foo", "   \n   \nFoo\n   \n   "),
                Arguments.of("center puts the rounding remainder at the bottom",
                        6, Position.Center, "Foo", "   \n   \nFoo\n   \n   \n   "),
                Arguments.of("center handles multiline content",
                        9, Position.Center, "Foo\nBar\nBaz", "   \n   \n   \nFoo\nBar\nBaz\n   \n   \n   "),

                // An explicit fractional position splits the gap proportionally and
                // keeps the total number of lines equal to the requested height.
                Arguments.of("fractional position splits proportionally",
                        5, new Position(0.25), "Foo", "   \n   \n   \nFoo\n   "),

                // Controls: the edge cases must stay on the documented branches.
                Arguments.of("top keeps content first", 5, Position.Top, "Foo", "Foo\n   \n   \n   \n   "),
                Arguments.of("bottom keeps content last", 5, Position.Bottom, "Foo", "   \n   \n   \n   \nFoo"),
                Arguments.of("no gap leaves content untouched", 1, Position.Center, "Foo", "Foo")
        );
    }

    @Test
    void test_ShouldPadWithoutAddingRows() {
        // when
        String placed = PlacementDecorator.placeVertical(5, Position.Center, "Foo");

        // then
        assertThat(placed.lines()).hasSize(5);
    }

    @Test
    void test_ShouldRespectWhitespaceChars() {
        // when
        String placed = PlacementDecorator.placeVertical(
                5,
                Position.Center,
                "Foo",
                Whitespace.WithWhitespaceChars(".")
        );

        // then
        assertThat(placed).isEqualTo("...\n...\nFoo\n...\n...");
    }

    @Test
    void test_ShouldPlaceTextInABox() {
        // when
        String placed = PlacementDecorator.place(
                8,
                5,
                Position.Center,
                Position.Center,
                "Foo",
                Whitespace.WithWhitespaceChars(".")
        );

        // then
        assertThat(placed).isEqualTo("........\n........\n..Foo...\n........\n........");
    }
}
