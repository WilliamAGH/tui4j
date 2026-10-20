package com.williamcallahan.tui4j.compat.bubbles.list;

import com.williamcallahan.tui4j.compat.bubbletea.BatchMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.SequenceMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the default list item delegate.
 * <p>
 * Upstream: bubbles/list/defaultitem.go, whose {@code Render} keeps the item
 * within {@code Height()} rows: the description is truncated to
 * {@code height - 1} lines so that title plus description never exceed the
 * height the list paginates by.
 */
class DefaultDelegateTest {

    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    void test_ShouldRenderTheItemWithinItsDeclaredHeight() {
        // given
        DefaultDelegate delegate = new DefaultDelegate();
        List list = createList(delegate, "first", "first desc", "second", "line-one\nline-two\nline-three");

        // when
        String rendered = render(delegate, list, 1, "second", "line-one\nline-two\nline-three");

        // then
        assertThat(rendered).isEqualTo("  second\n  line-one");
        assertThat(rendered.split("\n", -1)).hasSize(delegate.height());
    }

    @Test
    void test_ShouldKeepAShortDescription() {
        // given
        DefaultDelegate delegate = new DefaultDelegate();
        List list = createList(delegate, "first", "first desc", "second", "only line");

        // when
        String rendered = render(delegate, list, 1, "second", "only line");

        // then
        assertThat(rendered).isEqualTo("  second\n  only line");
    }

    @Test
    void test_ShouldRenderHeightMinusOneDescriptionLinesForATallerItem() {
        // given
        DefaultDelegate delegate = new DefaultDelegate();
        delegate.setHeight(3);
        List list = createList(delegate, "first", "first desc", "second", "line-one\nline-two\nline-three");

        // when
        String rendered = render(delegate, list, 1, "second", "line-one\nline-two\nline-three");

        // then
        assertThat(rendered).isEqualTo("  second\n  line-one\n  line-two");
        assertThat(rendered.split("\n", -1)).hasSize(delegate.height());
    }

    private static String render(DefaultDelegate delegate, List list, int index, String title, String description) {
        StringBuilder output = new StringBuilder();
        delegate.render(output, list, index, new FilteredItem(0, new TestItem(title, description), new int[0]));
        return output.toString();
    }

    private static List createList(DefaultDelegate delegate, String... titlesAndDescriptions) {
        Item[] items = new Item[titlesAndDescriptions.length / 2];
        for (int i = 0; i < items.length; i++) {
            items[i] = new TestItem(titlesAndDescriptions[i * 2], titlesAndDescriptions[i * 2 + 1]);
        }

        List list = new List(items, delegate, 30, 20);
        applyCommand(list, list.init());
        applyCommand(list, list.setShowTitle(false));
        list.setShowStatusBar(false);
        list.setShowHelp(false);
        list.setShowPagination(false);
        return list;
    }

    private static void applyCommand(List list, Command command) {
        if (Command.isNone(command)) {
            return;
        }
        applyMessage(list, command.execute());
    }

    private static void applyMessage(List list, Message msg) {
        if (msg == null) {
            return;
        }
        if (msg instanceof BatchMessage batchMessage) {
            for (Command command : batchMessage.commands()) {
                applyCommand(list, command);
            }
            return;
        }
        if (msg instanceof SequenceMessage sequenceMessage) {
            for (Command command : sequenceMessage.commands()) {
                applyCommand(list, command);
            }
            return;
        }

        String className = msg.getClass().getSimpleName();
        if (msg instanceof com.williamcallahan.tui4j.compat.bubbles.spinner.TickMessage
                || className.equals("InitialBlinkMessage")
                || className.equals("BlinkMessage")) {
            return;
        }

        UpdateResult<List> result = list.update(msg);
        if (result != null) {
            applyCommand(list, result.command());
        }
    }

    private record TestItem(String title, String description) implements DefaultItem {
        @Override
        public String filterValue() {
            return title;
        }
    }
}
