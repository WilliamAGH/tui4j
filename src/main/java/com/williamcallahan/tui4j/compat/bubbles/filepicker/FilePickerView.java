package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbles.filepicker.FilePicker.DirEntry;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders the file picker view.
 * <p>
 * Port of {@code Model.View} in
 * github.com/charmbracelet/bubbles/filepicker/filepicker.go: rows between
 * {@code min} and {@code max} render with a cursor column, optional
 * permissions and size columns, and a resolved symlink arrow; the selected
 * row pads its size to the {@code FileSize} style width like upstream's
 * {@code fmt.Sprintf("%<w>s", size)}.
 */
final class FilePickerView {

    private static final String EMPTY_MSG = "Bummer. No Files Found.";

    private static final Logger logger = Logger.getLogger(FilePickerView.class.getName());

    private FilePickerView() {
    }

    /**
     * Renders the full picker view for the model's current state.
     *
     * @param picker model state to render
     * @return rendered view
     */
    static String render(FilePicker picker) {
        List<DirEntry> files = picker.files();
        StringBuilder sb = new StringBuilder();

        if (files.isEmpty()) {
            sb.append(
                picker.styles()
                    .emptyDirectory()
                    .height(picker.height())
                    .render(EMPTY_MSG)
            );
            return sb.toString();
        }

        for (int i = 0; i < files.size(); i++) {
            if (i < picker.window().min() || i > picker.window().max()) {
                continue;
            }
            DirEntry f = files.get(i);
            boolean disabled = !picker.canSelect(f.name()) && !f.isDir();
            sb.append(renderRow(picker, i, f, disabled));
        }

        int currentHeight = sb.toString().split("\n", -1).length;
        for (int i = currentHeight; i <= picker.height(); i++) {
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Renders one listing row, selected or not, with cursor, permission,
     * size, and symlink-arrow columns.
     *
     * @param picker   model state
     * @param i        row index
     * @param f        entry to render
     * @param disabled whether selection is disabled for the entry
     * @return rendered row including trailing newline
     */
    private static String renderRow(
        FilePicker picker,
        int i,
        DirEntry f,
        boolean disabled
    ) {
        StringBuilder sb = new StringBuilder();
        if (picker.window().selected() == i) {
            StringBuilder selectedBuilder = new StringBuilder();
            if (picker.showPermissions()) {
                selectedBuilder.append(" ").append(f.permissions());
            }
            if (picker.showSize()) {
                selectedBuilder.append(paddedSize(f.size(), picker));
            }
            selectedBuilder.append(" ").append(f.name());

            String arrow = symlinkArrow(picker, f);
            if (!arrow.isEmpty()) {
                selectedBuilder.append(" → ").append(arrow);
            }

            if (disabled) {
                sb.append(picker.styles().disabledCursor().render(picker.cursorChar()));
                sb.append(
                    picker.styles().disabledSelected().render(selectedBuilder.toString())
                );
            } else {
                sb.append(picker.styles().cursor().render(picker.cursorChar()));
                sb.append(
                    picker.styles().selected().render(selectedBuilder.toString())
                );
            }
            sb.append("\n");
            return sb.toString();
        }

        Style style = picker.styles().file();
        if (f.isDir()) {
            style = picker.styles().directory();
        } else if (f.isSymlink()) {
            style = picker.styles().symlink();
        } else if (disabled) {
            style = picker.styles().disabledFile();
        }

        sb.append(picker.styles().cursor().render(" "));

        String fileName = style.render(f.name());
        String arrow = symlinkArrow(picker, f);
        if (!arrow.isEmpty()) {
            fileName += " → " + arrow;
        }

        if (picker.showPermissions()) {
            sb.append(" ").append(picker.styles().permission().render(f.permissions()));
        }
        if (picker.showSize()) {
            sb.append(picker.styles().fileSize().render(HumanBytes.format(f.size())));
        }
        sb.append(" ").append(fileName);
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Pads the size label to the {@code FileSize} style width like upstream's
     * {@code fmt.Sprintf("%<w>s", size)} on the selected row.
     *
     * @param size   entry size in bytes
     * @param picker model providing the style
     * @return right-aligned size label
     */
    private static String paddedSize(long size, FilePicker picker) {
        String label = HumanBytes.format(size);
        int width = picker.styles().fileSize().getWidth();
        return String.format("%" + Math.max(0, width) + "s", label);
    }

    /**
     * Resolves the {@code " → target"} payload upstream appends via
     * {@code filepath.EvalSymlinks} (fully resolved absolute path).
     *
     * @param picker model providing the current directory
     * @param f      entry to resolve
     * @return resolved target path, or empty string when not a symlink or
     *         unresolvable
     */
    private static String symlinkArrow(FilePicker picker, DirEntry f) {
        if (!f.isSymlink()) {
            return "";
        }
        try {
            Path linkPath = Paths.get(picker.currentDirectory(), f.name());
            return linkPath.toRealPath().toString();
        } catch (IOException e) {
            logger.log(
                Level.WARNING,
                "Failed to resolve symlink for " + f.name(),
                e
            );
            return "";
        }
    }
}
