package com.williamcallahan.tui4j.compat.bubbles.textarea;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the textarea word wrap grid.
 * <p>
 * Expected rows are the output of upstream {@code wrap} in
 * {@code bubbles/textarea/textarea.go} for the same inputs: a row keeps the
 * spaces that follow its last word, a word wider than the line is broken, and the
 * final row is padded with one extra space that the cursor navigation measures.
 */
class TextAreaWrapTest {

    @ParameterizedTest(name = "\"{0}\" at width {1}")
    @MethodSource("wrapCases")
    void test_ShouldWrapLikeUpstream(String input, int width, List<String> expectedRows) {
        List<int[]> rows = TextAreaWrap.wrap(TextAreaRunes.toCodePoints(input), width);

        assertThat(rows.stream().map(TextAreaRunes::toString).toList()).isEqualTo(expectedRows);
    }

    @Test
    void test_ShouldNotWrapWhenTheWidthIsUnset() {
        List<int[]> rows = TextAreaWrap.wrap(TextAreaRunes.toCodePoints("hello"), 0);

        assertThat(rows).hasSize(1);
        assertThat(TextAreaRunes.toString(rows.get(0))).isEqualTo("hello");
    }

    private static Stream<Arguments> wrapCases() {
        return Stream.of(
                Arguments.of("", 10, List.of(" ")),
                Arguments.of("a", 10, List.of("a ")),
                Arguments.of("abc", 5, List.of("abc ")),
                Arguments.of("hello", 5, List.of("hello", " ")),
                Arguments.of("hello world", 5, List.of("hello", " ", "world", " ")),
                Arguments.of("aaaaaaaaaaaa", 5, List.of("aaaaa", "aaaaa", "aa ")),
                Arguments.of("ab", 1, List.of("a", "b", " ")),
                Arguments.of("  leading", 6, List.of("  ", "leadin", "g ")),
                Arguments.of("trailing  ", 6, List.of("traili", "ng   ")),
                Arguments.of("one two three four five", 8, List.of("one two ", "three ", "four ", "five ")),
                Arguments.of("你好你好", 20, List.of("你好你好 ")),
                Arguments.of("This is a really long line that should wrap around the text area.", 20,
                        List.of("This is a really ", "long line that ", "should wrap around ", "the text area. "))
        );
    }
}
