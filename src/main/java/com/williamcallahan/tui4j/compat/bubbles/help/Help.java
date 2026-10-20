package com.williamcallahan.tui4j.compat.bubbles.help;

import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.lipgloss.Position;
import com.williamcallahan.tui4j.compat.lipgloss.Size;
import com.williamcallahan.tui4j.compat.lipgloss.join.HorizontalJoinDecorator;
import java.util.LinkedList;
import java.util.List;

/**
 * Port of Bubbles help.
 * Upstream: github.com/charmbracelet/bubbles/help/help.go
 */
public class Help {

    private int width;
    private boolean showAll;

    private String shortSeparator;
    private String fullSeparator;
    private String ellipsis;
    private Styles styles;

    /**
     * Creates a help renderer with default separators.
     */
    public Help() {
        this.shortSeparator = " • ";
        this.fullSeparator = "    ";
        this.ellipsis = "…";

        this.styles = new Styles();
    }

    /**
     * Sets the available width used for layout decisions.
     *
     * @param width terminal width in columns
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Renders the help view using the key map.
     *
     * @param keyMap key map to render
     * @return rendered help text
     */
    public String render(KeyMap keyMap) {
        if (showAll) {
            return fullHelpView(keyMap.fullHelp());
        }
        return shortHelpView(keyMap.shortHelp());
    }

    /**
     * Renders the full help view.
     *
     * @param groups grouped bindings
     * @return rendered help text
     */
    private String fullHelpView(Binding[][] groups) {
        if (groups.length == 0) {
            return "";
        }

        List<String> out = new LinkedList<>();
        int totalWidth = 0;
        String separator = styles
            .getFullSeparator()
            .copy()
            .inline(true)
            .render(fullSeparator);

        for (int i = 0; i < groups.length; i++) {
            Binding[] group = groups[i];
            if (group == null || !shouldRenderColumn(group)) {
                continue;
            }

            String sep = "";
            List<String> keys = new LinkedList<>();
            List<String> descriptions = new LinkedList<>();

            if (totalWidth > 0 && i < groups.length) {
                sep = separator;
            }

            for (Binding binding : group) {
                if (!binding.isEnabled()) {
                    continue;
                }

                keys.add(binding.help().key());
                descriptions.add(binding.help().desc());
            }

            // Column
            String col = HorizontalJoinDecorator.joinHorizontal(
                Position.Top,
                sep,
                styles.getFullKey().render(String.join("\n", keys)),
                " ",
                styles.getFullDesc().render(String.join("\n", descriptions))
            );
            int w = Size.width(col);

            // Tail
            Result result = shouldAddItem(totalWidth, w);
            if (!result.ok()) {
                if (!"".equals(result.tail())) {
                    out.add(result.tail());
                }
                break;
            }
            totalWidth += w;
            out.add(col);
        }
        return HorizontalJoinDecorator.joinHorizontal(
            Position.Top,
            out.toArray(new String[0])
        );
    }

    /**
     * Returns true when a column contains at least one enabled binding.
     *
     * @param group binding group
     * @return true when the group should render
     */
    private boolean shouldRenderColumn(Binding[] group) {
        for (Binding binding : group) {
            if (binding.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Renders the short help view.
     *
     * @param bindings bindings to render
     * @return rendered help text
     */
    private String shortHelpView(Binding[] bindings) {
        if (bindings == null || bindings.length == 0) {
            return "";
        }

        StringBuilder b = new StringBuilder();
        int totalWidth = 0;
        String separator = styles
            .getShortSeparator()
            .copy()
            .inline(true)
            .render(shortSeparator);

        for (int i = 0; i < bindings.length; i++) {
            Binding kb = bindings[i];
            if (!kb.isEnabled()) {
                continue;
            }

            // Sep
            String sep = "";
            if (totalWidth > 0 && i < bindings.length) {
                sep = separator;
            }

            // Item
            String str =
                sep +
                styles
                    .getShortKey()
                    .copy()
                    .inline(true)
                    .render(kb.help().key()) +
                " " +
                styles
                    .getShortDesc()
                    .copy()
                    .inline(true)
                    .render(kb.help().desc());
            int w = Size.width(str);

            // Tail
            Result result = shouldAddItem(totalWidth, w);
            if (!result.ok()) {
                if (!"".equals(result.tail())) {
                    b.append(result.tail());
                }
                break;
            }

            totalWidth += w;
            b.append(str);
        }

        return b.toString();
    }

    /**
     * Determines whether the next item fits within the width.
     * <p>
     * Upstream only shortens the view when it can render the ellipsis; when the
     * item overflows and the ellipsis does not fit, the item is still added and
     * the remaining items keep being rendered, so this must fall through to the
     * "add the item" result instead of dropping it.
     * <p>
     * Bubbles: help/help.go shouldAddItem.
     *
     * @param totalWidth current width
     * @param width next item width
     * @return result indicating fit and optional tail
     */
    private Result shouldAddItem(int totalWidth, int width) {
        if (this.width > 0 && totalWidth + width > this.width) {
            String tail =
                " " +
                styles.getEllipsis().copy().inline(true).render(this.ellipsis);
            if (totalWidth + Size.width(tail) < this.width) {
                return new Result(false, tail);
            }
        }
        return new Result(true, "");
    }

    /**
     * Sets the separator used between full help columns.
     *
     * @param fullSeparator separator string
     */
    public void setFullSeparator(String fullSeparator) {
        this.fullSeparator = fullSeparator;
    }

    /**
     * Sets whether to show the full help view.
     *
     * @param showAll true to show full help
     */
    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
    }

    /**
     * Returns whether the full help view is shown.
     *
     * @return true when full help is shown
     */
    public boolean showAll() {
        return showAll;
    }

    /**
     * Represents a layout decision for the next help item.
     *
     * @param ok whether the item fits
     * @param tail rendered tail content
     */
    record Result(boolean ok, String tail) {}
}
