package com.williamcallahan.tui4j.compat.bubbles.list;

import static com.williamcallahan.tui4j.compat.bubbles.list.DefaultItemStyles.ELLIPSIS;

import com.williamcallahan.tui4j.ansi.Truncate;
import com.williamcallahan.tui4j.compat.bubbles.paginator.Type;
import com.williamcallahan.tui4j.compat.lipgloss.Position;
import com.williamcallahan.tui4j.compat.lipgloss.Size;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.join.VerticalJoinDecorator;
import java.util.ArrayList;

/**
 * View rendering for {@link List}.
 * <p>
 * Upstream: bubbles/list/list.go (extracted from the original port).
 */
final class ListViewRenderer {

    private ListViewRenderer() {
    }

    static String view(List list) {
        java.util.List<String> sections = new ArrayList<>();
        int availHeight = list.height;

        if (list.showTitle || (list.showFilter && list.filteringEnabled)) {
            String v = titleView(list);
            sections.add(v);
            availHeight -= Size.height(v);
        }

        if (list.showStatusBar) {
            String v = statusView(list);
            sections.add(v);
            availHeight -= Size.height(v);
        }

        boolean showPaginationSection =
            list.showPagination && list.paginator.totalPages() > 1;
        String pagination = null;
        if (showPaginationSection) {
            pagination = paginationView(list);
            availHeight -= Size.height(pagination);
        }

        String helpSection = null;
        if (list.showHelp) {
            helpSection = helpView(list);
            availHeight -= Size.height(helpSection);
        }

        String content = Style.newStyle()
            .height(availHeight)
            .render(populatedView(list));
        sections.add(content);

        if (showPaginationSection) {
            sections.add(pagination);
        }

        if (list.showHelp) {
            sections.add(helpSection);
        }

        return VerticalJoinDecorator.joinVertical(
            Position.Left,
            sections.toArray(new String[0])
        );
    }

    static String titleView(List list) {
        StringBuilder view = new StringBuilder();
        Styles styles = list.styles();
        Style titleBarStyle = styles.titleBar().copy();
        String spinnerView = spinnerView(list);
        int spinnerWidth = Size.width(spinnerView);
        String spinnerLeftGap = " ";
        boolean spinnerOnLeft =
            titleBarStyle.leftPadding() >=
                spinnerWidth + Size.width(spinnerLeftGap) &&
                list.showSpinner;

        if (list.showFilter && list.filterState == FilterState.Filtering) {
            view.append(list.filterInput.view());
        } else if (list.showTitle) {
            if (list.showSpinner && spinnerOnLeft) {
                view.append(spinnerView).append(spinnerLeftGap);
                int titleBarGap = titleBarStyle.leftPadding();
                titleBarStyle = titleBarStyle.paddingLeft(
                    titleBarGap - spinnerWidth - Size.width(spinnerLeftGap)
                );
            }

            view.append(styles.title().render(list.title));
            if (list.filterState != FilterState.Filtering) {
                view.append("  ").append(list.statusMessage);
                view = new StringBuilder(
                    Truncate.truncate(
                        view.toString(),
                        list.width - spinnerWidth,
                        ELLIPSIS
                    )
                );
            }
        }

        if (list.showSpinner && !spinnerOnLeft) {
            int availSpace =
                list.width - Size.width(styles.titleBar().render(view.toString()));
            if (availSpace > spinnerWidth) {
                view
                    .append(" ".repeat(availSpace - spinnerWidth))
                    .append(spinnerView);
            }
        }

        if (!view.isEmpty()) {
            return titleBarStyle.render(view.toString());
        }
        return view.toString();
    }

    static String statusView(List list) {
        StringBuilder status = new StringBuilder();
        long matchedCount = list.matchedItems;

        String itemName =
            matchedCount == 1 ? list.itemNameSingular : list.itemNamePlural;

        String itemsDisplay = "%d %s".formatted(matchedCount, itemName);
        Styles styles = list.styles();

        if (list.filterState == FilterState.Filtering) {
            if (matchedCount == 0) {
                status = new StringBuilder(
                    styles.statusEmpty().render("Nothing matched")
                );
            } else {
                status = new StringBuilder(itemsDisplay);
            }
        } else if (list.totalItems == 0) {
            status = new StringBuilder(
                styles.statusEmpty().render("No " + list.itemNamePlural)
            );
        } else {
            boolean filtered = list.filterState == FilterState.FilterApplied;

            if (filtered) {
                String f = list.filterInput.value().trim();
                f = Truncate.truncate(f, 10, ELLIPSIS);
                status.append("“%s” ".formatted(f));
            }

            status.append(itemsDisplay);
        }

        if (
            list.filterState == FilterState.Filtering ||
                list.filterState == FilterState.FilterApplied
        ) {
            long numFiltered = list.totalItems - matchedCount;
            if (numFiltered > 0) {
                status
                    .append(styles.dividerDot().render())
                    .append(
                        styles
                            .statusBarFilterCount()
                            .render("%d filtered".formatted(numFiltered))
                    );
            }
        }

        return styles.statusBar().render(status.toString());
    }

    static String paginationView(List list) {
        Styles styles = list.styles();
        Style style = styles.paginationStyle().copy();
        if (list.itemDelegate.spacing() == 0 && style.topMargin() == 0) {
            style = style.marginTop(1);
        }

        if (list.paginator.totalPages() < 2) {
            return style.render("");
        }

        list.paginator.setType(Type.Dots);
        String view = list.paginator.view();
        if (Size.width(view) > list.width) {
            list.paginator.setType(Type.Arabic);
            view = styles.arabicPagination().render(list.paginator.view());
        }

        return style.render(view);
    }

    static String populatedView(List list) {
        StringBuilder b = new StringBuilder();
        Styles styles = list.styles();

        if (list.matchedItems == 0) {
            if (list.filterState == FilterState.Filtering) {
                return "";
            }
            return styles.noItems().render("No " + list.itemNamePlural + ".");
        }

        for (int i = 0; i < list.currentPageItems.size(); i++) {
            list.itemDelegate.render(
                b,
                list,
                list.paginator.page() * list.paginator.perPage() + i,
                list.currentPageItems.get(i)
            );
            if (i != list.currentPageItems.size() - 1) {
                b.append("\n".repeat(list.itemDelegate.spacing() + 1));
            }
        }

        int itemsOnPage = list.currentPageItems.size();
        if (itemsOnPage < list.paginator.perPage()) {
            int emptyLines =
                (list.paginator.perPage() - itemsOnPage) *
                    (list.itemDelegate.height() + list.itemDelegate.spacing());
            b.append("\n".repeat(emptyLines));
        }
        return b.toString();
    }

    static String helpView(List list) {
        return list.styles().helpStyle().render(list.help.render(list));
    }

    static String spinnerView(List list) {
        return list.spinner.view();
    }
}

