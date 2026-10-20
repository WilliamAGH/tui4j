package com.williamcallahan.tui4j.compat.lipgloss.tree;

import com.williamcallahan.tui4j.compat.lipgloss.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hierarchical tree data structure.
 * <p>
 * Port of github.com/charmbracelet/lipgloss/tree/tree.go.
 * Represents a node in a tree that can be rendered as a nested list.
 * <p>
 * Lipgloss: tree/tree.go.
 */
public class Tree implements Node {

    private String value;
    private boolean hidden;
    private int[] offset = new int[2];
    private Children children = new NodeChildren();
    private Renderer renderer;
    private boolean rendererSet;
    private ReentrantLock rendererLock = new ReentrantLock();

    /**
     * Creates an empty tree node.
     */
    public Tree() {
    }

    /**
     * Reports whether hidden.
     *
     * @return whether hidden
     */
    @Override
    public boolean isHidden() {
        return this.hidden;
    }

    /**
     * Handles hide for this component.
     *
     * @return result
     */
    public Tree hide() {
        this.hidden = true;
        return this;
    }

    /**
     * Handles hide for this component.
     *
     * @param hide hide
     * @return result
     */
    public Tree hide(boolean hide) {
        this.hidden = hide;
        return this;
    }

    /**
     * Updates the hidden.
     *
     * @param hidden hidden
     * @return result
     */
    public Tree setHidden(boolean hidden) {
        return hide(hidden);
    }

    /**
     * Handles offset for this component.
     *
     * @param start start
     * @param end end
     * @return result
     */
    public Tree offset(int start, int end) {
        int newStart = start;
        int newEnd = end;

        if (newStart > newEnd) {
            int temp = newStart;
            newStart = newEnd;
            newEnd = temp;
        }

        if (newStart < 0) {
            newStart = 0;
        }
        if (newEnd < 0 || newEnd > children.length()) {
            newEnd = children.length();
        }

        offset[0] = newStart;
        offset[1] = newEnd;
        return this;
    }

    /**
     * Handles value for this component.
     *
     * @return result
     */
    @Override
    public String value() {
        return this.value;
    }

    /**
     * Updates the value.
     *
     * @param value value
     */
    private void setValue(String value) {
        this.value = value;
    }

    /**
     * Adds child nodes or values to this tree node.
     *
     * @param children children to append
     * @return this tree node
     */
    public Tree child(Object... children) {
        for (Object child : children) {
            switch (child) {
                case null -> {
                }
                case Tree tree -> {
                    EnsureParentResult ensureParentResult = ensureParent(this.children, tree);
                    if (ensureParentResult.whatever() >= 0) {
                        this.children.remove(ensureParentResult.whatever());
                    }
                    this.children.append(ensureParentResult.tree());
                }
                case Children nestedChildren -> {
                    for (int i = 0; i < nestedChildren.length(); i++) {
                        this.children.append(nestedChildren.at(i));
                    }
                }
                case Node node -> this.children.append(node);
                case String string -> this.children.append(new Leaf(string));
                case Object[] objects -> {
                    return child(objects);
                }
                default -> this.children.append(new Leaf(child.toString()));
            }
        }
        return this;
    }

    /**
     * Handles ensure parent for this component.
     *
     * @param nodes nodes
     * @param item item
     * @return result
     */
    private EnsureParentResult ensureParent(Children nodes, Tree item) {
        int nodesLength = nodes.length();
        if ((item.value() != null && !item.value().isEmpty()) || nodesLength == 0) {
            return new EnsureParentResult(item, -1);
        }

        int j = nodesLength - 1;
        Node parent = nodes.at(j);
        if (parent instanceof Tree tree) {
            for (int i = 0; i < item.children().length(); i++) {
                tree.child(item.children().at(i));
            }
            return new EnsureParentResult(tree, j);  // Return parent tree instead of null
        } else if (parent instanceof Leaf leaf) {
            item.setValue(leaf.value());
            return new EnsureParentResult(item, j);
        }
        return new EnsureParentResult(item, -1);
    }

    /**
     * Handles ensure renderer for this component.
     *
     * @return result
     */
    private Renderer ensureRenderer() {
        rendererLock.lock();
        try {
            if (this.rendererSet) {
                return this.renderer;
            }
            this.rendererSet = true;
            return this.renderer = new Renderer();
        } finally {
            rendererLock.unlock();
        }
    }

    /**
     * Handles enumerator style for this component.
     *
     * @param style style
     * @return result
     */
    public Tree enumeratorStyle(Style style) {
        ensureRenderer().style().setEnumeratorFunction((children, index) -> style);
        return this;
    }

    /**
     * Handles enumerator style func for this component.
     *
     * @param function function
     * @return result
     */
    public Tree enumeratorStyleFunc(StyleFunction function) {
        StyleFunction fn = function;
        if (fn == null) {
            fn = (children, index) -> Style.newStyle();
        }
        ensureRenderer().style().setEnumeratorFunction(fn);
        return this;
    }

    /**
     * Handles root style for this component.
     *
     * @param style style
     * @return result
     */
    public Tree rootStyle(Style style) {
        ensureRenderer().style().setRootStyle(style);
        return this;
    }

    /**
     * Handles item style for this component.
     *
     * @param style style
     * @return result
     */
    public Tree itemStyle(Style style) {
        ensureRenderer().style().setItemFunction((children, index) -> style);
        return this;
    }

    /**
     * Handles item style func for this component.
     *
     * @param function function
     * @return result
     */
    public Tree itemStyleFunc(StyleFunction function) {
        StyleFunction fn = function;
        if (fn == null) {
            fn = (children, index) -> Style.newStyle();
        }
        ensureRenderer().style().setItemFunction(fn);
        return this;
    }

    /**
     * Handles enumerator for this component.
     *
     * @param enumerator enumerator
     * @return result
     */
    public Tree enumerator(TreeEnumerator enumerator) {
        ensureRenderer().setEnumerator(enumerator);
        return this;
    }

    /**
     * Handles indenter for this component.
     *
     * @param indenter indenter
     * @return result
     */
    public Tree indenter(TreeIndenter indenter) {
        ensureRenderer().setIndenter(indenter);
        return this;
    }

    /**
     * Handles with root for this component.
     *
     * @param root root
     * @return result
     */
    public static Tree withRoot(Object root) {
        Tree tree = new Tree();
        return tree.root(root);
    }

    /**
     * Handles new leaf for this component.
     *
     * @param value value
     * @param hidden hidden
     * @return result
     */
    public static Leaf newLeaf(Object value, boolean hidden) {
        return new Leaf(value, hidden);
    }

    /**
     * Sets the root value of this tree.
     * <p>
     * A node value adopts that node's root value and children, matching upstream, where
     * {@code SetValue} delegates to {@code Root}.
     * Lipgloss: tree/tree.go.
     *
     * @param value value to set
     * @return this tree
     */
    public Tree setValue(Object value) {
        return root(value);
    }

    /**
     * Handles root for this component.
     *
     * @param root root
     * @return result
     */
    public Tree root(Object root) {
        switch (root) {
            case Tree tree -> {
                this.value = tree.value();
                child(tree.children());
            }
            case Node node -> this.value = node.value();
            case String string -> this.value = string;
            case null, default -> this.value = "" + root;
        }
        return this;
    }

    /**
     * Handles children for this component.
     *
     * @return result
     */
    @Override
    public Children children() {
        List<Node> children = new ArrayList<>();
        for (int i = offset[0]; i < this.children.length() - offset[1]; i++) {
            children.add(this.children.at(i));
        }
        return new NodeChildren(children);
    }


    /**
     * Handles renderer for this component.
     *
     * @return result
     */
    Renderer renderer() {
        return renderer;
    }

    /**
     * Renders the tree hierarchy to a string.
     *
     * @return rendered tree
     */
    public String render() {
        return ensureRenderer().render(this, true, "");
    }

    /**
     * Renders this tree to a string, matching upstream's {@code Tree.String()}, which every
     * node type must provide because the upstream {@code Node} interface embeds
     * {@code fmt.Stringer}.
     * Lipgloss: tree/tree.go.
     *
     * @return rendered tree
     */
    @Override
    public String toString() {
        return render();
    }

    /**
     * Compatibility port of EnsureParentResult to preserve upstream behavior.
     * <p>
     * Lipgloss: tree/tree.go.
     */
    private record EnsureParentResult(Tree tree, int whatever) {
    }
}
