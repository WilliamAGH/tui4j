package com.williamcallahan.tui4j.compat.x.ansi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ANSI-aware hard wrapping.
 */
class HardWrapTest {

    /** Validates HardWrap behavior against upstream cases. */
    @Test
    void testHardWrapCases() {
        List<Case> cases = List.of(
                new Case("empty string", "", 0, "", true),
                new Case("passthrough", "foobar\n ", 0, "foobar\n ", true),
                new Case("pass", "foo", 4, "foo", true),
                new Case("simple", "foobarfoo", 4, "foob\narfo\no", true),
                new Case("lf", "f\no\nobar", 3, "f\no\noba\nr", true),
                new Case("lf_space", "foo bar\n  baz", 3, "foo\n ba\nr\n  b\naz", true),
                new Case("tab", "foo\tbar", 3, "foo\n\tbar", true),
                new Case("unicode_space", "foo\u00a0bar", 3, "foo\nbar", false),
                // U+0085 (NEL) is Unicode White_Space, which upstream's unicode.IsSpace
                // uses, so it is skipped as a leading space like an ASCII space.
                new Case("nel_leading_space", "\u0085abc", 10, "abc", false),
                new Case("nel_kept_when_preserving", "\u0085abc", 10, "\u0085abc", true),
                new Case("nel_inside_line", "ab\u0085cd", 4, "ab\u0085cd", false),
                new Case("style_nochange",
                        "\u001B[38;2;249;38;114mfoo\u001B[0m\u001B[38;2;248;248;242m \u001B[0m\u001B[38;2;230;219;116mbar\u001B[0m",
                        7,
                        "\u001B[38;2;249;38;114mfoo\u001B[0m\u001B[38;2;248;248;242m \u001B[0m\u001B[38;2;230;219;116mbar\u001B[0m",
                        true),
                new Case("style",
                        "\u001B[38;2;249;38;114m(\u001B[0m\u001B[38;2;248;248;242mjust another test\u001B[38;2;249;38;114m)\u001B[0m",
                        3,
                        "\u001B[38;2;249;38;114m(\u001B[0m\u001B[38;2;248;248;242mju\nst \nano\nthe\nr t\nest\u001B[38;2;249;38;114m\n)\u001B[0m",
                        true),
                new Case("style_lf",
                        "I really \u001B[38;2;249;38;114mlove\u001B[0m Go!",
                        8,
                        "I really\n\u001B[38;2;249;38;114mlove\u001B[0m Go!",
                        false),
                new Case("style_emoji",
                        "I really \u001B[38;2;249;38;114mlove u\uD83E\uDEAB\u001B[0m",
                        8,
                        "I really\n\u001B[38;2;249;38;114mlove u\uD83E\uDEAB\u001B[0m",
                        false),
                new Case("hyperlink",
                        "I really \u001B]8;;https://example.com/\u001B\\love\u001B]8;;\u001B\\ Go!",
                        10,
                        "I really \u001B]8;;https://example.com/\u001B\\l\nove\u001B]8;;\u001B\\ Go!",
                        false),
                new Case("dcs",
                        "\u001BPq#0;2;0;0;0#1;2;100;100;0#2;2;0;100;0#1~~@@vv@@~~@@~~$#2??}}GG}}??}}??-#1!14@\u001B\\foobar",
                        3,
                        "\u001BPq#0;2;0;0;0#1;2;100;100;0#2;2;0;100;0#1~~@@vv@@~~@@~~$#2??}}GG}}??}}??-#1!14@\u001B\\foo\nbar",
                        false),
                new Case("begin_with_space", " foo", 4, " foo", false),
                new Case("style_dont_affect_wrap",
                        "\u001B[38;2;249;38;114mfoo\u001B[0m\u001B[38;2;248;248;242m \u001B[0m\u001B[38;2;230;219;116mbar\u001B[0m",
                        7,
                        "\u001B[38;2;249;38;114mfoo\u001B[0m\u001B[38;2;248;248;242m \u001B[0m\u001B[38;2;230;219;116mbar\u001B[0m",
                        false),
                new Case("preserve_style",
                        "\u001B[38;2;249;38;114m(\u001B[0m\u001B[38;2;248;248;242mjust another test\u001B[38;2;249;38;114m)\u001B[0m",
                        3,
                        "\u001B[38;2;249;38;114m(\u001B[0m\u001B[38;2;248;248;242mju\nst \nano\nthe\nr t\nest\u001B[38;2;249;38;114m\n)\u001B[0m",
                        false),
                new Case("emoji", "foo\uD83E\uDEABfoobar", 4, "foo\n\uD83E\uDEABfo\nobar", false),
                new Case("osc8_wrap",
                        "สวัสดีสวัสดี\u001B]8;;https://example.com\u001B\\สวัสดีสวัสดี\u001B]8;;\u001B\\",
                        8,
                        "สวัสดีสวัสดี\u001B]8;;https://example.com\u001B\\\nสวัสดีสวัสดี\u001B]8;;\u001B\\",
                        false),
                new Case("column", "VERTICAL", 1, "V\nE\nR\nT\nI\nC\nA\nL", false),
                new Case("nel_leading_space_column", "\u0085ab", 1, "a\nb", false)
        );

        for (Case testCase : cases) {
            String actual = HardWrap.hardWrap(testCase.input, testCase.limit, testCase.preserveSpace);
            assertEquals(testCase.expected, actual, testCase.name);
        }
    }

    /** Test case for hard wrap. */
    private static final class Case {
        private final String name;
        private final String input;
        private final int limit;
        private final String expected;
        private final boolean preserveSpace;

        /**
         * Creates a hard wrap test case.
         *
         * @param name case name
         * @param input input string
         * @param limit wrap width
         * @param expected expected output
         * @param preserveSpace whether to preserve leading space
         */
        private Case(String name, String input, int limit, String expected, boolean preserveSpace) {
            this.name = name;
            this.input = input;
            this.limit = limit;
            this.expected = expected;
            this.preserveSpace = preserveSpace;
        }
    }
}
