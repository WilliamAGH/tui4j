package com.williamcallahan.tui4j.compat.bubbles.list.fuzzy;

import com.williamcallahan.tui4j.compat.bubbles.list.Rank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fuzzy matching filter for list items.
 * <p>
 * Port of the {@code bubbles/list} filter functions, which delegate the matching
 * itself to {@link Fuzzy}: {@link #defaultFilter(String, String[])} ranks the
 * candidates by match quality, {@link #unsortedFilter(String, String[])} keeps
 * them in the order they were given.
 *
 * @see <a href="https://github.com/charmbracelet/bubbles/blob/v0.21.0/list/list.go">bubbles/list/list.go</a>
 * <p>
 * Bubbles: list/list.go.
 */
public class FuzzyFilter {

    /**
     * Creates FuzzyFilter to keep this component ready for use.
     * <p>
     * Protected to allow legacy compatibility shims.
     */
    protected FuzzyFilter() {}

    /**
     * Filters and ranks targets by fuzzy matching against a search term.
     * Results are sorted by match quality, best first; candidates that score the
     * same keep their original order.
     *
     * @param term the search term
     * @param targets the strings to match against
     * @return ranked results for matching items
     */
    public static Rank[] defaultFilter(String term, String[] targets) {
        List<Match> matches = new ArrayList<>(Fuzzy.find(term, targets));
        matches.sort(Comparator.comparingInt(Match::getScore).reversed());
        return ranks(matches);
    }

    /**
     * Filters targets by fuzzy matching without sorting.
     *
     * @param term the search term
     * @param targets the strings to match against
     * @return ranked results in original order
     */
    public static Rank[] unsortedFilter(String term, String[] targets) {
        return ranks(Fuzzy.find(term, targets));
    }

    /**
     * Projects fuzzy matches onto the list filter contract.
     *
     * @param matches matches to project
     * @return ranks in match order
     */
    private static Rank[] ranks(List<Match> matches) {
        Rank[] ranks = new Rank[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            ranks[i] = new Rank(match.getIndex(), matchedIndexes(match));
        }
        return ranks;
    }

    /**
     * Copies the matched character positions of a match.
     *
     * @param match match to read
     * @return matched character indexes
     */
    private static int[] matchedIndexes(Match match) {
        List<Integer> positions = match.getMatchedIndexes();
        int[] indexes = new int[positions.size()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = positions.get(i);
        }
        return indexes;
    }
}
