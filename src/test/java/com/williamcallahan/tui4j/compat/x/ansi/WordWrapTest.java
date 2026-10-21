package com.williamcallahan.tui4j.compat.x.ansi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ANSI-aware word wrapping.
 */
class WordWrapTest {

    /** Validates WordWrap behavior against upstream cases. */
    @Test
    void testWordWrapCases() {
        List<Case> cases = List.of(
                new Case("empty string", "", 0, "", ""),
                new Case("passthrough", "foobar\n ", 0, "", "foobar\n "),
                new Case("pass", "foo", 3, "", "foo"),
                new Case("toolong", "foobarfoo", 4, "", "foobarfoo"),
                new Case("white space", "foo bar foo", 4, "", "foo\nbar\nfoo"),
                new Case("broken_at_spaces", "foo bars foobars", 4, "", "foo\nbars\nfoobars"),
                new Case("hyphen", "foo-foobar", 4, "-", "foo-\nfoobar"),
                new Case("emoji_breakpoint", "foo😃 foobar", 4, "😃", "foo😃\nfoobar"),
                new Case("wide_emoji_breakpoint", "foo\uD83E\uDEAB foobar", 4, "🫧", "foo\uD83E\uDEAB\nfoobar"),
                new Case("space_breakpoint", "foo --bar", 9, "-", "foo --bar"),
                new Case("simple", "foo bars foobars", 4, "", "foo\nbars\nfoobars"),
                new Case("limit", "foo bar", 5, "", "foo\nbar"),
                new Case("remove white spaces", "foo    \nb   ar   ", 4, "", "foo\nb\nar"),
                new Case("white space trail width", "foo\nb\t a\n bar", 4, "", "foo\nb\t a\n bar"),
                new Case("explicit_line_break", "foo bar foo\n", 4, "", "foo\nbar\nfoo\n"),
                new Case("explicit_breaks", "\nfoo bar\n\n\nfoo\n", 4, "", "\nfoo\nbar\n\n\nfoo\n"),
                new Case("example",
                        " This is a list: \n\n\t* foo\n\t* bar\n\n\n\t* foo  \nbar    ",
                        6,
                        "",
                        " This\nis a\nlist: \n\n\t* foo\n\t* bar\n\n\n\t* foo\nbar"),
                new Case("style_code_dont_affect_length",
                        "\u001B[38;2;249;38;114mfoo\u001B[0m\u001B[38;2;248;248;242m \u001B[0m\u001B[38;2;230;219;116mbar\u001B[0m",
                        7,
                        "",
                        "\u001B[38;2;249;38;114mfoo\u001B[0m\u001B[38;2;248;248;242m \u001B[0m\u001B[38;2;230;219;116mbar\u001B[0m"),
                new Case("style_code_dont_get_wrapped",
                        "\u001B[38;2;249;38;114m(\u001B[0m\u001B[38;2;248;248;242mjust another test\u001B[38;2;249;38;114m)\u001B[0m",
                        3,
                        "",
                        "\u001B[38;2;249;38;114m(\u001B[0m\u001B[38;2;248;248;242mjust\nanother\ntest\u001B[38;2;249;38;114m)\u001B[0m"),
                new Case("osc8_wrap",
                        "สวัสดีสวัสดี\u001B]8;;https://example.com\u001B\\ สวัสดีสวัสดี\u001B]8;;\u001B\\",
                        8,
                        "",
                        "สวัสดีสวัสดี\u001B]8;;https://example.com\u001B\\\nสวัสดีสวัสดี\u001B]8;;\u001B\\")
        );

        for (Case testCase : cases) {
            String actual = WordWrap.wordWrap(testCase.input, testCase.limit, testCase.breakpoints);
            assertEquals(testCase.expected, actual, testCase.name);
        }
    }

    /**
     * Tests that breakpoints match by codepoint, not bytes.
     * When comparing UTF-8 bytes instead of codepoints, different emoji characters
     * that share byte prefixes would falsely match as breakpoints.
     */
    @Test
    void testBreakpointCodepointMatching() {
        // 😃 (U+1F603) and 🫧 (U+1FAE7) are different emoji but their UTF-8 encodings share leading bytes
        // This test ensures we compare by codepoint, not bytes

        // Should NOT break at 🫧 when breakpoint is 😃 - they are different emoji
        String textWithBubble = "foo🫧bar";
        String result = WordWrap.wordWrap(textWithBubble, 4, "😃");
        assertEquals("foo🫧bar", result, "Should not break at 🫧 when breakpoint is 😃");

        // SHOULD break at 😃 when breakpoint is 😃
        String textWithSmiley = "foo😃bar";
        result = WordWrap.wordWrap(textWithSmiley, 4, "😃");
        assertEquals("foo😃\nbar", result, "Should break at 😃 when breakpoint is 😃");
    }

    /**
     * Tests that wide whitespace (U+3000 - ideographic space) is counted by display width
     * not byte length when determining wrap points. U+3000 has display width 2 but is 3 bytes in UTF-8.
     * When wrap happens, the space that triggered it is discarded (same as regular spaces).
     */
    @Test
    void testWideWhitespaceWidthTracking() {
        // U+3000 (ideographic space) is 3 bytes in UTF-8 but display width 2
        // Text: "あ　い" - あ (width 2) + wide space (width 2) + い (width 2) = 6 total width
        // With limit 4, あ(2) + wide space(2) = 4, then い would make 6 > 4, so wrap happens
        // The wide space is discarded when it causes the wrap (same as regular space behavior)

        String textWithWideSpaces = "あ\u3000い";
        String result = WordWrap.wordWrap(textWithWideSpaces, 4, "");
        assertEquals("あ\nい", result,
            "Wide whitespace counted by display width (2), not byte length (3). Wraps correctly at limit 4.");

        // Verify byte-based width would give wrong result:
        // If we used byte length: "あ" (3 bytes) + "\u3000" (3 bytes) = 6 bytes
        // With limit 4, a byte-based implementation would break prematurely at wrong position.
    }

    /**
     * Tests that having multiple different emoji as breakpoints doesn't cause false matches
     * due to shared byte prefixes in UTF-8 encoding.
     */
    @Test
    void testMultipleEmojiBreakpoints() {
        // Mix of emoji with different codepoints but potentially overlapping byte sequences
        String breakpoints = "😃🫧🔥🎉";

        // Should break at 😃
        String result = WordWrap.wordWrap("hello😃world", 6, breakpoints);
        assertEquals("hello😃\nworld", result, "Should break at matching emoji 😃");

        // Should break at 🫧
        result = WordWrap.wordWrap("hello🫧world", 6, breakpoints);
        assertEquals("hello🫧\nworld", result, "Should break at matching emoji 🫧");

        // Should break at 🔥
        result = WordWrap.wordWrap("hello🔥world", 6, breakpoints);
        assertEquals("hello🔥\nworld", result, "Should break at matching emoji 🔥");

        // Should break at 🎉
        result = WordWrap.wordWrap("hello🎉world", 6, breakpoints);
        assertEquals("hello🎉\nworld", result, "Should break at matching emoji 🎉");

        // Should NOT break at other emoji not in breakpoints
        result = WordWrap.wordWrap("hello🎄world", 6, breakpoints);
        assertEquals("hello🎄world", result, "Should not break at 🎄 (not in breakpoints)");
    }

    /**
     * U+0085 (NEL) is Unicode White_Space, which is what upstream's unicode.IsSpace
     * uses, so it breaks a word like an ASCII space does.
     */
    @Test
    void testNelIsBreakableWhitespace() {
        assertEquals("ab\ncd", WordWrap.wordWrap("ab\u0085cd", 2, ""),
                "NEL should break the word like a space");
        assertEquals("你c\nced\n你d", WordWrap.wordWrapWc("你c  ced\u0085你d", 5, ""),
                "NEL should break the wide-character line like a space");
    }

    /**
     * U+00A0 (NBSP) is whitespace but never a breakable one, so it stays inside
     * the word and the line is not split there.
     */
    @Test
    void testNbspIsNotBreakableWhitespace() {
        assertEquals("ab\u00a0cd", WordWrap.wordWrap("ab\u00a0cd", 2, ""),
                "NBSP should stay inside the word");
    }

    /** Test case for word wrap. */
    private static final class Case {
        private final String name;
        private final String input;
        private final int limit;
        private final String breakpoints;
        private final String expected;

        /**
         * Creates a word wrap test case.
         *
         * @param name case name
         * @param input input string
         * @param limit wrap width
         * @param breakpoints breakpoint string
         * @param expected expected output
         */
        private Case(String name, String input, int limit, String breakpoints, String expected) {
            this.name = name;
            this.input = input;
            this.limit = limit;
            this.breakpoints = breakpoints;
            this.expected = expected;
        }
    }
}
