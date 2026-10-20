package com.williamcallahan.tui4j.compat.bubbles.list.fuzzy;

import java.util.ArrayList;
import java.util.List;

/**
 * Fuzzy matching for filenames and code symbols, in the style of Sublime Text.
 * <p>
 * Port of {@code github.com/sahilm/fuzzy@v0.1.1} {@code fuzzy.FindFromNoSort},
 * the matcher behind {@code bubbles/list}'s default filter. A candidate matches
 * when the term is a case-insensitive subsequence of it. Every term character is
 * matched at the position that scores best so far — first character, camel case,
 * following a separator and adjacent to the previous match each add a bonus —
 * and candidates are ranked against a penalty for leading and unmatched
 * characters.
 * <p>
 * Positions are reported in characters rather than upstream's UTF-8 bytes. The
 * consumer contract is character based: {@code bubbles/list} stores these
 * indexes as "rune indices of matched items" and hands them to
 * {@code lipgloss.StyleRunes}, which indexes runes, so byte offsets would
 * highlight the wrong characters of a non-ASCII item.
 * <p>
 * Fuzzy: github.com/sahilm/fuzzy/blob/v0.1.1/fuzzy.go
 */
final class Fuzzy {

    private static final int FIRST_CHAR_MATCH_BONUS = 10;
    private static final int MATCH_FOLLOWING_SEPARATOR_BONUS = 20;
    private static final int CAMEL_CASE_MATCH_BONUS = 20;
    private static final int ADJACENT_MATCH_BONUS = 5;
    private static final int UNMATCHED_LEADING_CHAR_PENALTY = -5;
    private static final int MAX_UNMATCHED_LEADING_CHAR_PENALTY = -15;

    /**
     * Characters that separate words in a candidate.
     */
    private static final String SEPARATORS = "/-_ .\\";

    private Fuzzy() {
    }

    /**
     * Finds the targets that contain the term as a subsequence, in target order.
     *
     * @param term term to match, no matches when empty
     * @param targets candidates to match against
     * @return matches ordered by their position in {@code targets}
     */
    static List<Match> find(String term, String[] targets) {
        if (term.isEmpty()) {
            return List.of();
        }

        int[] pattern = term.codePoints().toArray();
        List<Match> matches = new ArrayList<>();
        for (int index = 0; index < targets.length; index++) {
            Match match = match(targets[index], index, pattern);
            if (match != null) {
                matches.add(match);
            }
        }
        return matches;
    }

    /**
     * Matches one candidate against the pattern.
     *
     * @param target candidate to match
     * @param index position of the candidate in the target list
     * @param pattern term code points
     * @return the match, or {@code null} when the pattern is not a subsequence
     */
    private static Match match(String target, int index, int[] pattern) {
        int[] runes = target.codePoints().toArray();
        List<Integer> matchedIndexes = new ArrayList<>(pattern.length);
        int score = 0;
        int totalScore = 0;
        int patternIndex = 0;
        int bestScore = -1;
        int matchedIndex = -1;
        int adjacentMatchBonus = 0;
        int last = 0;
        int lastIndex = 0;

        for (int j = 0; j < runes.length; j++) {
            int candidate = runes[j];
            if (equalFold(candidate, pattern[patternIndex])) {
                score = 0;
                if (j == 0) {
                    score += FIRST_CHAR_MATCH_BONUS;
                }
                if (Character.isLowerCase(last) && Character.isUpperCase(candidate)) {
                    score += CAMEL_CASE_MATCH_BONUS;
                }
                if (j != 0 && isSeparator(last)) {
                    score += MATCH_FOLLOWING_SEPARATOR_BONUS;
                }
                if (!matchedIndexes.isEmpty()) {
                    int lastMatch = matchedIndexes.get(matchedIndexes.size() - 1);
                    int bonus = adjacentCharBonus(lastIndex, lastMatch, adjacentMatchBonus);
                    score += bonus;
                    // Adjacent matches are incremental and keep growing from the
                    // previous adjacent bonus, so it has to be carried forward.
                    adjacentMatchBonus += bonus;
                }
                if (score > bestScore) {
                    bestScore = score;
                    matchedIndex = j;
                }
            }

            int nextPattern = patternIndex < pattern.length - 1 ? pattern[patternIndex + 1] : 0;
            int next = j + 1 < runes.length ? runes[j + 1] : 0;

            // Committing when the next candidate also matches the next pattern
            // character (or the candidate ran out) lets the search take the best
            // position rather than merely the first one.
            if (equalFold(nextPattern, next) || next == 0) {
                if (matchedIndex > -1) {
                    if (matchedIndexes.isEmpty()) {
                        int penalty = matchedIndex * UNMATCHED_LEADING_CHAR_PENALTY;
                        bestScore += Math.max(penalty, MAX_UNMATCHED_LEADING_CHAR_PENALTY);
                    }
                    totalScore += bestScore;
                    matchedIndexes.add(matchedIndex);
                    score = 0;
                    bestScore = -1;
                    patternIndex++;
                }
            }

            lastIndex = j;
            last = candidate;
        }

        // One point of penalty for every character of the candidate that the
        // term did not match.
        totalScore += matchedIndexes.size() - runes.length;
        if (matchedIndexes.size() != pattern.length) {
            return null;
        }
        return new Match(target, index, matchedIndexes, totalScore);
    }

    /**
     * Scores a match against the previous match position.
     *
     * @param position position of the character being matched
     * @param lastMatch position of the previous match
     * @param currentBonus bonus accumulated by the previous adjacent match
     * @return the adjacency bonus, {@code 0} when the match is not adjacent
     */
    private static int adjacentCharBonus(int position, int lastMatch, int currentBonus) {
        if (lastMatch == position) {
            return currentBonus * 2 + ADJACENT_MATCH_BONUS;
        }
        return 0;
    }

    /**
     * Returns whether the character separates words in a candidate.
     *
     * @param character character to test
     * @return true when the character is a separator
     */
    private static boolean isSeparator(int character) {
        return SEPARATORS.indexOf(character) >= 0;
    }

    /**
     * Compares two characters ignoring case, as {@code strings.EqualFold} does.
     *
     * @param first first character
     * @param second second character
     * @return true when both characters fold to the same case-folded character
     */
    private static boolean equalFold(int first, int second) {
        if (first == second) {
            return true;
        }
        return Character.toUpperCase(Character.toLowerCase(first))
                == Character.toUpperCase(Character.toLowerCase(second));
    }
}
