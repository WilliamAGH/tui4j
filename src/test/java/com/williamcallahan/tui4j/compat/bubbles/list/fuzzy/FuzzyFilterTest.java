package com.williamcallahan.tui4j.compat.bubbles.list.fuzzy;

import com.williamcallahan.tui4j.compat.bubbles.list.Rank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests fuzzy filter.
 *
 * <p>Expectations are the output of the upstream Go code this filter ports:
 * {@code github.com/sahilm/fuzzy@v0.1.1} ({@code fuzzy.Find} and
 * {@code fuzzy.FindNoSort}) as called by
 * {@code github.com/charmbracelet/bubbles@v0.21.0/list/list.go}
 * ({@code DefaultFilter} and {@code UnsortedFilter}).</p>
 */
class FuzzyFilterTest {

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testDefaultFilter(String term, String[] targets, Rank[] expected) {
        Rank[] result = FuzzyFilter.defaultFilter(term, targets);
        assertArrayEquals(expected, result, "Failed for term: " + term + ", targets: " + Arrays.toString(targets));
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
                arguments("mnr", new String[]{"moduleNameResolver.ts"},
                        new Rank[]{new Rank(0, new int[]{0, 6, 10})}),
                arguments("mmt", new String[]{"mémeTemps"},
                        new Rank[]{new Rank(0, new int[]{0, 2, 4})}),
                // The shorter candidate wins the same term: it carries fewer
                // unmatched characters, so it scores higher.
                arguments("mnr", new String[]{"moduleNameResolver.ts", "my name is_Ramsey"},
                        new Rank[]{
                                new Rank(1, new int[]{0, 3, 11}),
                                new Rank(0, new int[]{0, 6, 10})
                        }),
                arguments("mmr", new String[]{"moduleNameResolver.ts", "my name is_Ramsey"},
                        new Rank[]{
                                new Rank(1, new int[]{0, 5, 11}),
                                new Rank(0, new int[]{0, 8, 10})
                        }),
                // A term character is matched at the best position, not the first
                // one: 'k' prefers the camel case 'K' of "Knight" over the 'k' of
                // "Black".
                arguments("tk", new String[]{"The Black Knight"},
                        new Rank[]{new Rank(0, new int[]{0, 10})}),
                arguments("tkk", new String[]{"The Black Knight"},
                        new Rank[]{new Rank(0, new int[]{0, 8, 10})}),
                // Leading, adjacent matches outrank a later match.
                arguments("ai", new String[]{"ai", "aim", "main"},
                        new Rank[]{
                                new Rank(0, new int[]{0, 1}),
                                new Rank(1, new int[]{0, 1}),
                                new Rank(2, new int[]{1, 2})
                        }),
                // Separators boost the character that follows them.
                arguments("fba", new String[]{"foo/bar/baz"},
                        new Rank[]{new Rank(0, new int[]{0, 4, 5})}),
                arguments("dtt", new String[]{"do_mounts_rd.c", "date.2025.txt"},
                        new Rank[]{new Rank(1, new int[]{0, 2, 10})}),
                // Quality ordering across candidates: "log.go" beats "blog", and
                // both beat the late match inside "catalog.go".
                arguments("log", new String[]{"catalog.go", "log.go", "blog"},
                        new Rank[]{
                                new Rank(1, new int[]{0, 1, 4}),
                                new Rank(2, new int[]{1, 2, 3}),
                                new Rank(0, new int[]{4, 5, 8})
                        }),
                arguments("ip", new String[]{"pipe.go", "tip.go", "clipboard.go", "ip.go"},
                        new Rank[]{
                                new Rank(3, new int[]{0, 1}),
                                new Rank(1, new int[]{1, 2}),
                                new Rank(0, new int[]{1, 2}),
                                new Rank(2, new int[]{2, 3})
                        }),
                arguments("aaa", new String[]{"aaa", "bbb"},
                        new Rank[]{new Rank(0, new int[]{0, 1, 2})}),
                arguments("abc", new String[]{"abc", "ab"},
                        new Rank[]{new Rank(0, new int[]{0, 1, 2})}),
                arguments("abcx", new String[]{"abc\\x"},
                        new Rank[]{new Rank(0, new int[]{0, 1, 2, 4})}),
                arguments("cats", new String[]{"cat"}, new Rank[]{}),
                arguments("m", new String[]{"foo", "bar"}, new Rank[]{}),
                arguments("", new String[]{"cat"}, new Rank[]{}));
    }

    @Test
    void testUnsortedFilterKeepsTargetOrder() {
        String[] targets = {"moduleNameResolver.ts", "my name is_Ramsey"};

        Rank[] result = FuzzyFilter.unsortedFilter("mnr", targets);

        assertArrayEquals(
                new Rank[]{new Rank(0, new int[]{0, 6, 10}), new Rank(1, new int[]{0, 3, 11})},
                result,
                "unsorted filter must keep the order the targets were given in");
    }

    @Test
    void testUnsortedFilter() {
        String[] targets = {"foo", "bar", "foobar"};

        Rank[] result = FuzzyFilter.unsortedFilter("foo", targets);

        assertArrayEquals(
                new Rank[]{new Rank(0, new int[]{0, 1, 2}), new Rank(2, new int[]{0, 1, 2})},
                result,
                "unsorted filter must skip non-matching targets and keep the rest in order");
    }

    @Test
    void matchedIndexesCountCharactersNotBytes() {
        // Upstream reports UTF-8 byte offsets here. The port reports character
        // indexes, which is the contract bubbles/list documents ("rune indices of
        // matched items") and what its StyleRunes match highlighting consumes.
        assertArrayEquals(new int[]{0, 2, 4},
                FuzzyFilter.defaultFilter("mmt", new String[]{"mémeTemps"})[0].getMatchedIndexes());
        assertArrayEquals(new int[]{6, 7, 8},
                FuzzyFilter.defaultFilter("tea", new String[]{"emoji🧋tea"})[0].getMatchedIndexes());
    }

    @Test
    void testDefaultFilterEmptyTargets() {
        Rank[] result = FuzzyFilter.defaultFilter("test", new String[]{});
        assertEquals(0, result.length, "Empty targets should return empty array");
    }

    @Test
    void testUnsortedFilterEmptyTargets() {
        Rank[] result = FuzzyFilter.unsortedFilter("test", new String[]{});
        assertEquals(0, result.length, "Empty targets should return empty array");
    }

    @Test
    void testDefaultFilterNullTargetsThrowsException() {
        assertThrows(NullPointerException.class, () -> FuzzyFilter.defaultFilter("test", null));
    }

    @Test
    void testUnsortedFilterNullTargetsThrowsException() {
        assertThrows(NullPointerException.class, () -> FuzzyFilter.unsortedFilter("test", null));
    }
}
