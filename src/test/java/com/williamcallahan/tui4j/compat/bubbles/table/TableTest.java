package com.williamcallahan.tui4j.compat.bubbles.table;

import com.williamcallahan.tui4j.compat.lipgloss.Borders;
import com.williamcallahan.tui4j.compat.lipgloss.Position;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.compat.x.ansi.Strip;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of github.com/charmbracelet/bubbles/table/table_test.go.
 */
class TableTest {

    private static final Column[] TEST_COLUMNS = new Column[]{
            new Column("col1", 10),
            new Column("col2", 10),
            new Column("col3", 10)
    };

    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer().setColorProfile(ColorProfile.Ascii);
    }

    @Test
    void testDefaults() {
        Table table = Table.create();

        assertThat(table.cursor()).isZero();
        assertThat(table.getColumns()).isEmpty();
        assertThat(table.getRows()).isEmpty();
        assertThat(table.width()).isEqualTo(80);
        assertThat(table.height()).isEqualTo(20);
        assertThat(table.focused()).isFalse();
        assertThat(table.keyMap()).isNotNull();
        assertThat(table.styles()).isNotNull();
    }

    @Test
    void testWithColumns() {
        Table table = Table.create()
                .columns(new Column("Foo", 1), new Column("Bar", 2));

        assertThat(table.getColumns()).containsExactly(
                new Column("Foo", 1),
                new Column("Bar", 2)
        );
    }

    @Test
    void testWithColumnsAndRows() {
        Table table = Table.create()
                .columns(new Column("Foo", 1), new Column("Bar", 2))
                .rows(new Row("1", "Foo"), new Row("2", "Bar"));

        assertThat(table.getColumns()).containsExactly(
                new Column("Foo", 1),
                new Column("Bar", 2)
        );
        assertThat(table.getRows()).containsExactly(
                new Row("1", "Foo"),
                new Row("2", "Bar")
        );
    }

    @Test
    void testWithHeight() {
        Table table = Table.create().height(10);
        assertThat(table.height()).isEqualTo(10);
    }

    @Test
    void testWithWidth() {
        Table table = Table.create().width(10);
        assertThat(table.width()).isEqualTo(10);
    }

    @Test
    void testWithFocused() {
        Table table = Table.create().focused(true);
        assertThat(table.focused()).isTrue();
    }

    @Test
    void testWithStyles() {
        Styles styles = new Styles();
        Table table = Table.create().styles(styles);
        assertThat(table.styles()).isSameAs(styles);
    }

    @Test
    void testWithKeyMap() {
        Keys keys = new Keys();
        Table table = Table.create().keyMap(keys);
        assertThat(table.keyMap()).isSameAs(keys);
    }

    @Test
    void testFromValues() {
        String input = "foo1,bar1\nfoo2,bar2\nfoo3,bar3";
        Table table = Table.create().columns(new Column("Foo"), new Column("Bar"));
        table.fromValues(input, ",");

        assertThat(table.getRows()).containsExactly(
                new Row("foo1", "bar1"),
                new Row("foo2", "bar2"),
                new Row("foo3", "bar3")
        );
    }

    @Test
    void testFromValuesWithTabSeparator() {
        String input = "foo1.\tbar1\nfoo,bar,baz\tbar,2";
        Table table = Table.create().columns(new Column("Foo"), new Column("Bar"));
        table.fromValues(input, "\t");

        assertThat(table.getRows()).containsExactly(
                new Row("foo1.", "bar1"),
                new Row("foo,bar,baz", "bar,2")
        );
    }

    @Test
    void testRenderRow() throws Exception {
        record RenderCase(String name, Row[] rows, String expected) {
        }

        List<RenderCase> cases = List.of(
                new RenderCase(
                        "simple row",
                        new Row[]{new Row("Foooooo", "Baaaaar", "Baaaaaz")},
                        "Foooooo   Baaaaar   Baaaaaz   "
                ),
                new RenderCase(
                        "simple row with truncations",
                        new Row[]{new Row("Foooooooooo", "Baaaaaaaaar", "Quuuuuuuuux")},
                        "Foooooooo…Baaaaaaaa…Quuuuuuuu…"
                ),
                new RenderCase(
                        "simple row avoiding truncations",
                        new Row[]{new Row("Fooooooooo", "Baaaaaaaar", "Quuuuuuuux")},
                        "FoooooooooBaaaaaaaarQuuuuuuuux"
                )
        );

        for (RenderCase renderCase : cases) {
            Table table = Table.create()
                    .columns(TEST_COLUMNS)
                    .rows(renderCase.rows())
                    .styles(new Styles().cell(Style.newStyle()).selected(Style.newStyle()));

            String row = renderRow(table, 0);
            assertThat(row).as(renderCase.name()).isEqualTo(renderCase.expected());
        }
    }

    @Test
    void testTableAlignment() {
        Table noBorder = Table.create()
                .height(5)
                .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                .rows(
                        new Row("Chocolate Digestives", "UK", "Yes"),
                        new Row("Tim Tams", "Australia", "No"),
                        new Row("Hobnobs", "UK", "Yes")
                );
        assertGolden("TestTableAlignment/No_border", Strip.strip(noBorder.view()));

        Style baseStyle = Style.newStyle()
                .border(Borders.normalBorder())
                .borderForeground(Color.color("240"));

        Styles styles = Styles.defaultStyles();
        styles.header(
                styles.header()
                        .border(Borders.normalBorder())
                        .borderForeground(Color.color("240"))
                        .borderBottom(true)
                        .bold(false)
        );

        Table withBorder = Table.create()
                .height(5)
                .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                .rows(
                        new Row("Chocolate Digestives", "UK", "Yes"),
                        new Row("Tim Tams", "Australia", "No"),
                        new Row("Hobnobs", "UK", "Yes")
                )
                .styles(styles);

        assertGolden("TestTableAlignment/With_border", Strip.strip(baseStyle.render(withBorder.view())));
    }

    @Test
    void testCursorNavigation() {
        record CursorCase(String name, List<Row> rows, Consumer<Table> action, int expectedCursor) {
        }

        List<Row> basicRows = List.of(
                new Row("r1"),
                new Row("r2"),
                new Row("r3"),
                new Row("r4")
        );

        List<CursorCase> cases = List.of(
                new CursorCase("New", List.of(new Row("r1"), new Row("r2"), new Row("r3")), table -> {
                }, 0),
                new CursorCase("MoveDown", basicRows, table -> table.moveDown(2), 2),
                new CursorCase("MoveUp", basicRows, table -> {
                    table.setCursor(3);
                    table.moveUp(2);
                }, 1),
                new CursorCase("GotoBottom", basicRows, Table::gotoBottom, 3),
                new CursorCase("GotoTop", basicRows, table -> {
                    table.setCursor(3);
                    table.gotoTop();
                }, 0),
                new CursorCase("SetCursor", basicRows, table -> table.setCursor(2), 2),
                new CursorCase("MoveDown with overflow", basicRows, table -> table.moveDown(5), 3),
                new CursorCase("MoveUp with overflow", basicRows, table -> {
                    table.setCursor(3);
                    table.moveUp(5);
                }, 0),
                new CursorCase("Blur does not stop movement", basicRows, table -> {
                    table.blur();
                    table.moveDown(2);
                }, 2)
        );

        for (CursorCase cursorCase : cases) {
            Table table = Table.create().columns(TEST_COLUMNS).rows(cursorCase.rows());
            cursorCase.action().accept(table);
            assertThat(table.cursor()).as(cursorCase.name()).isEqualTo(cursorCase.expectedCursor());
        }
    }

    @Test
    void testSetRows() {
        Table table = Table.create().columns(TEST_COLUMNS);

        assertThat(table.getRows()).isEmpty();

        table.rows(new Row("r1"), new Row("r2"));

        assertThat(table.getRows()).containsExactly(new Row("r1"), new Row("r2"));
    }

    @Test
    void testSetColumns() {
        Table table = Table.create();

        assertThat(table.getColumns()).isEmpty();

        table.columns(new Column("Foo"), new Column("Bar"));

        assertThat(table.getColumns()).containsExactly(new Column("Foo"), new Column("Bar"));
    }

    @Test
    void testView() {
        record ViewCase(String name, Supplier<Table> modelFunc, boolean skip) {
        }

        List<ViewCase> cases = List.of(
                new ViewCase("Empty", Table::create, false),
                new ViewCase("Single_row_and_column", () -> Table.create()
                        .columns(new Column("Name", 25))
                        .rows(new Row("Chocolate Digestives")), false),
                new ViewCase("Multiple_rows_and_columns", () -> Table.create()
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        ), false),
                new ViewCase("Extra_padding", () -> {
                    Styles styles = Styles.defaultStyles();
                    styles.header(Style.newStyle().padding(2, 2));
                    styles.cell(Style.newStyle().padding(2, 2));
                    return Table.create()
                            .height(10)
                            .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                            .rows(
                                    new Row("Chocolate Digestives", "UK", "Yes"),
                                    new Row("Tim Tams", "Australia", "No"),
                                    new Row("Hobnobs", "UK", "Yes")
                            )
                            .styles(styles);
                }, false),
                new ViewCase("No_padding", () -> {
                    Styles styles = Styles.defaultStyles();
                    styles.header(Style.newStyle());
                    styles.cell(Style.newStyle());
                    return Table.create()
                            .height(10)
                            .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                            .rows(
                                    new Row("Chocolate Digestives", "UK", "Yes"),
                                    new Row("Tim Tams", "Australia", "No"),
                                    new Row("Hobnobs", "UK", "Yes")
                            )
                            .styles(styles);
                }, false),
                new ViewCase("Bordered_headers", () -> Table.create()
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        )
                        .styles(new Styles().header(Style.newStyle().border(Borders.normalBorder()))), false),
                new ViewCase("Bordered_cells", () -> Table.create()
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        )
                        .styles(new Styles().cell(Style.newStyle().border(Borders.normalBorder()))), false),
                new ViewCase("Manual_height_greater_than_rows", () -> Table.create()
                        .height(6)
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        ), false),
                new ViewCase("Manual_height_less_than_rows", () -> Table.create()
                        .height(2)
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        ), false),
                new ViewCase("Manual_width_greater_than_columns", () -> Table.create()
                        .width(80)
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        ), false),
                new ViewCase("Manual_width_less_than_columns", () -> Table.create()
                        .width(30)
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        ), true),
                new ViewCase("Modified_viewport_height", () -> Table.create()
                        .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                        .rows(
                                new Row("Chocolate Digestives", "UK", "Yes"),
                                new Row("Tim Tams", "Australia", "No"),
                                new Row("Hobnobs", "UK", "Yes")
                        )
                        .height(2), false)
        );

        for (ViewCase viewCase : cases) {
            if (viewCase.skip()) {
                continue;
            }
            Table table = viewCase.modelFunc().get();
            String view = Strip.strip(table.view());
            assertGolden("TestModel_View/" + viewCase.name(), view);
        }
    }

    @Disabled("Upstream test is skipped")
    @Test
    void testViewCenteredInABox() {
        Style boxStyle = Style.newStyle()
                .border(Borders.normalBorder())
                .align(Position.Center);

        Table table = Table.create()
                .height(6)
                .width(80)
                .columns(new Column("Name", 25), new Column("Country of Origin", 16), new Column("Dunk-able", 12))
                .rows(
                        new Row("Chocolate Digestives", "UK", "Yes"),
                        new Row("Tim Tams", "Australia", "No"),
                        new Row("Hobnobs", "UK", "Yes")
                );

        String tableView = Strip.strip(table.view());
        String got = boxStyle.render(tableView);

        assertGolden("TestModel_View_CenteredInABox", got);
    }

    private static String renderRow(Table table, int index) throws Exception {
        Method method = Table.class.getDeclaredMethod("renderRow", int.class);
        method.setAccessible(true);
        return (String) method.invoke(table, index);
    }

    private static void assertGolden(String name, String actual) {
        assertGoldenPath("bubbles/table/testdata/" + name + ".golden", actual);
    }

    private static void assertGoldenPath(String path, String actual) {
        String expected = readResource(path);
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * Upstream {@code table.DefaultStyles} renders the selected row with
     * {@code Bold(true).Foreground(lipgloss.Color("212"))}; the default style
     * must keep that ANSI-256 pink so default tables match bubbles output.
     */
    @Test
    void testDefaultSelectedStyleHasUpstreamForeground() {
        TerminalInfo.provide(() -> new TerminalInfo(true, new NoColor()));
        com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer()
                .setColorProfile(ColorProfile.ANSI256);

        String rendered = Styles.defaultStyles().selected().render("row");

        assertThat(rendered).contains("38;5;212");
    }

    private static String readResource(String path) {
        try (InputStream input = TableTest.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + path, e);
        }
    }
}
