package com.williamcallahan.tui4j.compat.bubbles.list;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.BatchMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.SequenceMessage;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of github.com/charmbracelet/bubbles/list/list_test.go.
 */
class ListTest {

    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    void testStatusBarItemName() {
        List list = createList(new TestItem("foo"), new TestItem("bar"));

        assertThat(list.view()).contains("2 items");

        updateItems(list, new TestItem("foo"));

        assertThat(list.view()).contains("1 item");
    }

    @Test
    void testStatusBarWithoutItems() {
        List list = createList();

        assertThat(list.view()).contains("No items");
    }

    @Test
    void testCustomStatusBarItemName() {
        List list = createList(new TestItem("foo"), new TestItem("bar"));
        list.setStatusBarItemName("connection", "connections");

        assertThat(list.view()).contains("2 connections");

        updateItems(list, new TestItem("foo"));
        assertThat(list.view()).contains("1 connection");

        updateItems(list);
        assertThat(list.view()).contains("No connections");
    }

    @Test
    void testSetFilterText() {
        List list = createList(new TestItem("foo"), new TestItem("bar"), new TestItem("baz"));

        applyCommand(list, list.setFilterText("ba"));

        applyCommand(list, list.setFilterState(FilterState.Unfiltered));
        assertThat(visibleValues(list)).containsExactly("foo", "bar", "baz");

        applyCommand(list, list.setFilterState(FilterState.Filtering));
        assertThat(visibleValues(list)).containsExactly("bar", "baz");

        applyCommand(list, list.setFilterState(FilterState.FilterApplied));
        assertThat(visibleValues(list)).containsExactly("bar", "baz");
    }

    @Test
    void testSetFilterState() {
        List list = createList(new TestItem("foo"), new TestItem("bar"), new TestItem("baz"));

        applyCommand(list, list.setFilterText("ba"));

        applyCommand(list, list.setFilterState(FilterState.Unfiltered));
        String footer = footerLine(list.view());
        assertThat(footer).contains("up").doesNotContain("clear filter");

        applyCommand(list, list.setFilterState(FilterState.Filtering));
        footer = footerLine(list.view());
        assertThat(footer).contains("filter").doesNotContain("more");

        applyCommand(list, list.setFilterState(FilterState.FilterApplied));
        footer = footerLine(list.view());
        assertThat(footer).contains("clear");
    }

    @Test
    void testResizeKeepsAbsoluteSelectionIndex() {
        Item[] items = new Item[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = new TestItem("item-" + i);
        }
        List list = createList(items);

        applyCommand(list, list.setShowTitle(false));
        list.setShowFilter(false);
        list.setShowStatusBar(false);
        list.setShowPagination(false);
        applyCommand(list, list.setShowHelp(false));
        applyCommand(list, list.refresh());

        applyCommand(list, list.select(80));
        assertThat(list.index()).isEqualTo(80);

        applyCommand(list, list.setSize(10, 5));
        assertThat(list.index()).isEqualTo(80);
        assertThat(list.selectedItem().filterValue()).isEqualTo("item-80");
    }

    @Test
    void testPopulatedViewDoesNotShowEmptyStateWhenMatchesExist() {
        ListDataSource dataSource = (page, perPage, filterValue) ->
            new FetchedItems(java.util.List.of(), 5, 5, 5);
        List list = new List(dataSource, new TestDelegate(), 10, 5);
        applyCommand(list, list.init());

        assertThat(list.view()).doesNotContain("No items.");
    }

    @Test
    void testAcceptFilteringUsesMatchedItemsNotCurrentPageSlice() {
        ListDataSource dataSource = (page, perPage, filterValue) ->
            new FetchedItems(java.util.List.of(), 3, 5, 1);
        List list = new List(dataSource, new TestDelegate(), 10, 5);
        applyCommand(list, list.init());
        applyCommand(list, list.setFilterText("x"));
        applyCommand(list, list.setFilterState(FilterState.Filtering));

        applyMessage(list, new KeyPressMessage(new Key(KeyType.keyCR)));

        assertThat(list.filterState()).isEqualTo(FilterState.FilterApplied);
    }

    @Test
    void testTitleViewDoesNotRenderSpinnerWhileFiltering() {
        // Upstream's titleView draws the left-aligned spinner only inside the title
        // branch, so an in-flight spinner never shifts the shown filter input
        // (bubbles/list titleView).
        List withSpinner = createList(new TestItem("foo"), new TestItem("bar"));
        List withoutSpinner = createList(new TestItem("foo"), new TestItem("bar"));

        applyCommand(withSpinner, withSpinner.setFilterState(FilterState.Filtering));
        applyCommand(withoutSpinner, withoutSpinner.setFilterState(FilterState.Filtering));

        // A filter fetch keeps the spinner running while the input is shown.
        withSpinner.startSpinner();

        assertThat(ListViewRenderer.titleView(withSpinner))
                .isEqualTo(ListViewRenderer.titleView(withoutSpinner));
    }

    @Test
    void testTitleViewSeparatesTheStatusMessageFromTheTitle() {
        // Upstream renders the title, two spaces, then the status message
        // (bubbles/list titleView), so the two never run together.
        List list = new List(new Item[]{new TestItem("foo")}, new TestDelegate(), 40, 10);
        applyCommand(list, list.init());
        list.newStatusMessage("hello");

        String titleLine = ListViewRenderer.titleView(list).split("\n", -1)[0];

        assertThat(titleLine).isEqualTo("   List   hello");
    }

    @Test
    void testEmptyResultsStayOnSinglePageWithPaginationDisabled() {
        ListDataSource dataSource = (page, perPage, filterValue) ->
            new FetchedItems(java.util.List.of(), 0, 0, 0);
        List list = new List(dataSource, new TestDelegate(), 10, 5);
        applyCommand(list, list.init());

        assertThat(Command.isNone(list.nextPage())).isTrue();
        assertThat(Command.isNone(list.prevPage())).isTrue();
    }

    @Test
    void testCursorUpFromLaterPageKeepsCursorNonNegativeWhenFetchedPageIsEmpty() {
        ListDataSource dataSource = (page, perPage, filterValue) -> {
            if (page == 1) {
                return new FetchedItems(
                    java.util.List.of(new FilteredItem(new TestItem("item-1"))),
                    2,
                    2,
                    2
                );
            }
            return new FetchedItems(java.util.List.of(), 2, 2, 2);
        };
        List list = new List(dataSource, new TestDelegate(), 10, 5);
        applyCommand(list, list.init());
        applyCommand(list, list.setShowTitle(false));
        list.setShowFilter(false);
        list.setShowStatusBar(false);
        list.setShowPagination(false);
        applyCommand(list, list.setShowHelp(false));
        applyCommand(list, list.setSize(10, 1));

        applyCommand(list, list.select(1));
        applyCommand(list, list.cursorUp());

        assertThat(list.cursor()).isZero();
        assertThat(list.index()).isZero();
    }

    @Test
    void testGoToEndSelectsTheLastItem() {
        // Upstream sets the page to the last page and the cursor to the last item on
        // it (`m.cursor = m.Paginator.ItemsOnPage(numItems) - 1`), so go-to-end
        // always selects the last item (bubbles/list handleBrowsing).
        List list = createPagedList(12);
        assertThat(Command.isNone(list.nextPage())).isFalse();
        applyCommand(list, list.select(0));
        assertThat(list.index()).isZero();

        applyMessage(list, new KeyPressMessage(new Key(KeyType.KeyEnd)));

        assertThat(list.index()).isEqualTo(11);
        assertThat(list.selectedItem().filterValue()).isEqualTo("item-11");
    }

    @Test
    void testGoToStartSelectsTheFirstItemWhileOnTheFirstPage() {
        // Upstream resets page and cursor unconditionally (`m.Paginator.Page = 0;
        // m.cursor = 0`), so go-to-start moves the selection even when the list
        // already shows the first page (bubbles/list handleBrowsing).
        List list = createPagedList(12);
        applyCommand(list, list.select(2));
        assertThat(list.index()).isEqualTo(2);

        applyMessage(list, new KeyPressMessage(new Key(KeyType.KeyHome)));

        assertThat(list.index()).isZero();
        assertThat(list.selectedItem().filterValue()).isEqualTo("item-0");
    }

    private static List createList(Item... items) {
        List list = new List(items, new TestDelegate(), 10, 10);
        applyCommand(list, list.init());
        return list;
    }

    private static List createPagedList(int count) {
        List list = createList(items(count));
        applyCommand(list, list.setShowTitle(false));
        list.setShowFilter(false);
        list.setShowStatusBar(false);
        list.setShowPagination(false);
        applyCommand(list, list.setShowHelp(false));
        applyCommand(list, list.refresh());
        return list;
    }

    private static Item[] items(int count) {
        Item[] items = new Item[count];
        for (int i = 0; i < count; i++) {
            items[i] = new TestItem("item-" + i);
        }
        return items;
    }

    private static void updateItems(List list, Item... items) {
        DefaultDataSource dataSource = (DefaultDataSource) list.dataSource();
        applyCommand(list, dataSource.setItems(items));
    }

    private static void applyCommand(List list, Command command) {
        if (Command.isNone(command)) {
            return;
        }
        applyMessage(list, command.execute());
    }

    private static final int MAX_RECURSION_DEPTH = 100;
    private static int recursionDepth = 0;

    private static void applyMessage(List list, Message msg) {
        if (msg == null) {
            return;
        }

        // Guard against infinite loops in test (real Program handles this async)
        if (recursionDepth++ > MAX_RECURSION_DEPTH) {
            recursionDepth = 0;
            return;
        }

        try {
            applyMessageInner(list, msg);
        } finally {
            recursionDepth--;
        }
    }

    private static void applyMessageInner(List list, Message msg) {
        // Avoid infinite time-based messages in unit tests (they're async in real programs).
        if (msg instanceof com.williamcallahan.tui4j.compat.bubbles.spinner.TickMessage) {
            return;
        }
        // Filter cursor blink messages (package-private, check by class name)
        String className = msg.getClass().getSimpleName();
        if (className.equals("InitialBlinkMessage") || className.equals("BlinkMessage")) {
            return;
        }

        // Bubble Tea: Program handles Batch/Sequence by executing nested commands.
        if (msg instanceof BatchMessage batchMessage) {
            for (Command c : batchMessage.commands()) {
                applyCommand(list, c);
            }
            return;
        }
        if (msg instanceof SequenceMessage sequenceMessage) {
            for (Command c : sequenceMessage.commands()) {
                applyCommand(list, c);
            }
            return;
        }

        com.williamcallahan.tui4j.compat.bubbletea.UpdateResult<List> result = list.update(msg);
        if (result != null && !Command.isNone(result.command())) {
            applyCommand(list, result.command());
        }
    }

    private static java.util.List<String> visibleValues(List list) {
        return list.visibleItems().stream()
                .map(item -> item.item().filterValue())
                .toList();
    }

    private static String footerLine(String view) {
        String[] lines = view.split("\n");
        return lines[lines.length - 1];
    }

    private record TestItem(String value) implements Item {
        @Override
        public String filterValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class TestDelegate implements ItemDelegate {
        @Override
        public void render(StringBuilder output, List list, int index, FilteredItem filteredItem) {
            output.append(index + 1).append(". ").append(filteredItem.item().filterValue());
        }

        @Override
        public int height() {
            return 1;
        }

        @Override
        public int spacing() {
            return 0;
        }

        @Override
        public Command update(Message msg, List listModel) {
            return Command.none();
        }
    }
}
