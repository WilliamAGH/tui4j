package com.williamcallahan.tui4j.compat.lipgloss;

import com.williamcallahan.tui4j.compat.lipgloss.color.AdaptiveColor;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.border.StandardBorder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer;

/**
 * Tests style.
 * Bubble Tea: bubbletea/examples/list-fancy/main.go
 */
class StyleTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("styleData")
    void test_ShouldRenderStyledText(String caseName, String input, Style style, String expectedOutput) {
        // when
        String rendered = style.render(input);

        // then
        assertThat(rendered).isEqualTo(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("inheritData")
    void testInheritCopiesStyle(Style base, Style child) {
        child.inherit(base);

        assertThat(child.render("hello")).isEqualTo(base.render("hello"));
    }

    private static Stream<Arguments> styleData() {
        Renderer renderer = defaultRenderer();
        renderer.setColorProfile(ColorProfile.TrueColor);
        renderer.setHasDarkBackground(true);

        return Stream.of(
                Arguments.of("empty", "hello", renderer.newStyle(), "hello"),
                Arguments.of("margin right", "foo", renderer.newStyle().marginRight(1), "foo "),
                Arguments.of("margin left", "foo", renderer.newStyle().marginLeft(1), " foo"),
                Arguments.of("empty text margin left", "", renderer.newStyle().marginLeft(1), " "),
                Arguments.of("empty text margin right", "", renderer.newStyle().marginRight(1), " "),
                // Top/bottom margin rows span the padded block width, including the
                // left/right margins themselves (lipgloss applyMargins pads first).
                Arguments.of("margin", "Hello, World!", renderer.newStyle().margin(1, 2, 3, 4),
                        "                   \n    Hello, World!  \n                   \n                   \n                   "),
                Arguments.of("padding and margin", "Foo", renderer.newStyle().padding(1).margin(1),
                        "       \n       \n  Foo  \n       \n       "),
                Arguments.of("color", "hello", renderer.newStyle().foreground(Color.color("#5A56E0")), "\u001B[38;2;90;86;224mhello\u001B[0m"),
                Arguments.of("adaptive color", "hello", renderer.newStyle().foreground(new AdaptiveColor("#fffe12", "#5A56E0")), "\u001B[38;2;90;86;224mhello\u001B[0m"),
                Arguments.of("bold", "hello", renderer.newStyle().bold(true), "\u001B[1mhello\u001B[0m"),
                Arguments.of("italic", "hello", renderer.newStyle().italic(true), "\u001B[3mhello\u001B[0m"),
                Arguments.of("underline", "hello", renderer.newStyle().underline(true), "\u001B[4mhello\u001B[0m"),
                Arguments.of("blink", "hello", renderer.newStyle().blink(true), "\u001B[5mhello\u001B[0m"),
                Arguments.of("faint", "hello", renderer.newStyle().faint(true), "\u001B[2mhello\u001B[0m"),
                Arguments.of("maxWidth truncation", "hello world", renderer.newStyle().maxWidth(5), "hello"),
                Arguments.of("maxWidth truncation with ellipsis", "hello world", renderer.newStyle().maxWidth(5).ellipsis("..."), "he..."),
                Arguments.of("maxWidth zero is unlimited", "hello world", renderer.newStyle().maxWidth(0), "hello world"),
                Arguments.of("maxHeight truncation", "line1\nline2\nline3\nline4", renderer.newStyle().maxHeight(2), "line1\nline2"),
                Arguments.of("maxHeight zero is unlimited", "line1\nline2\nline3", renderer.newStyle().maxHeight(0), "line1\nline2\nline3"),
                Arguments.of("maxWidth truncation with multiline text", "line1\nline22222\nline3", renderer.newStyle().maxWidth(5), "line1\nline2\nline3"),
                Arguments.of("maxHeight with border", "line1\nline2\nline3", renderer.newStyle().maxHeight(1).borderDecoration(StandardBorder.NormalBorder), "┌─────┐"),
                Arguments.of("maxHeight truncation with multiline", "line1\nline2\nline3\nline4\nline5", renderer.newStyle().maxHeight(3), "line1\nline2\nline3"),
                Arguments.of("maxWidth with multiline and ellipsis", "line1_extra\nline2_extra\nline3_extra", renderer.newStyle().maxWidth(5).ellipsis(".."), "lin..\nlin..\nlin.."),
                // A trailing newline owns a real final blank row: upstream splits with
                // strings.Split, which keeps trailing empty lines (lipgloss Style.Render).
                // The alignment property is set so that the style is not the zero style,
                // which upstream returns unchanged.
                Arguments.of("trailing newline", "foo\n", renderer.newStyle().align(Position.Left), "foo\n   "),
                Arguments.of("double trailing newline", "foo\n\n", renderer.newStyle().align(Position.Left), "foo\n   \n   "),
                Arguments.of("newline only", "\n", renderer.newStyle(), "\n"),
                Arguments.of("width with trailing newline", "hello world\n", renderer.newStyle().width(5), "hello\nworld\n     "),
                Arguments.of("padding with trailing newline", "foo\n", renderer.newStyle().padding(1), "     \n foo \n     \n     "),
                Arguments.of("padding left with trailing newline", "foo\n", renderer.newStyle().paddingLeft(2), "  foo\n     "),
                Arguments.of("padding and margin with trailing newline", "foo\n", renderer.newStyle().padding(1).margin(1),
                        "       \n       \n  foo  \n       \n       \n       "),
                Arguments.of("maxWidth with trailing newline", "hello\n", renderer.newStyle().maxWidth(3), "hel\n   "),
                Arguments.of("maxHeight with trailing newline", "a\nb\n", renderer.newStyle().maxHeight(3), "a\nb\n "),
                Arguments.of("maxHeight truncates the trailing newline", "a\nb\n", renderer.newStyle().maxHeight(2), "a\nb"),
                Arguments.of("border with trailing newline", "foo\n", renderer.newStyle().borderDecoration(StandardBorder.NormalBorder),
                        "┌───┐\n│foo│\n│   │\n└───┘")
        );
    }

    private static Stream<Arguments> inheritData() {
        Renderer renderer = defaultRenderer();
        renderer.setColorProfile(ColorProfile.TrueColor);
        renderer.setHasDarkBackground(true);

        Style base = renderer.newStyle()
                .foreground(Color.color("#5A56E0"))
                .background(Color.color("#0f0f0f"))
                .bold(true)
                .italic(true)
                .underline(true)
                .maxWidth(10)
                .ellipsis("...")
                .border(StandardBorder.NormalBorder, true)
                .align(Position.Center, Position.Bottom);

        return Stream.of(
                Arguments.of(base, renderer.newStyle())
        );
    }
}
