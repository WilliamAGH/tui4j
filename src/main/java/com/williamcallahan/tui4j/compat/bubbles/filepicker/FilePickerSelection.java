package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbles.filepicker.FilePicker.DirEntry;
import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Logger;

/**
 * Selection queries for the file picker.
 * <p>
 * Port of {@code DidSelectFile}/{@code didSelectFile} in
 * github.com/charmbracelet/bubbles/filepicker/filepicker.go, adapted to the
 * port's boolean-returning API by recording the chosen path on the model.
 */
final class FilePickerSelection {

    private static final Logger logger = Logger.getLogger(FilePickerSelection.class.getName());

    private FilePickerSelection() {
    }

    /**
     * Returns whether the key press selected an allowed file or directory
     * and records the path on the picker.
     *
     * @param picker model to query and update
     * @param keyMsg key press to inspect
     * @return true when selection occurred
     */
    static boolean didSelectFile(FilePicker picker, KeyPressMessage keyMsg) {
        if (!Binding.matches(keyMsg, picker.keyMap().select())) {
            return false;
        }

        List<DirEntry> files = picker.files();
        if (files.isEmpty()) {
            return false;
        }

        DirEntry f = files.get(picker.window().selected());
        boolean isDir;
        try {
            isDir = resolvesToDirectory(picker, f);
        } catch (IOException e) {
            logger.fine(() -> "Failed to resolve symlink for " + f.name());
            return false;
        }
        boolean selectable = (!isDir && picker.fileAllowed() && picker.canSelect(f.name()))
            || (isDir && picker.dirAllowed());

        if (selectable) {
            picker.path(
                Paths.get(picker.currentDirectory(), f.name()).toString()
            );
            return true;
        }
        return false;
    }

    /**
     * Returns whether the key press opened a directory entry.
     *
     * @param picker model to query
     * @param keyMsg key press to inspect
     * @return true when the entry is (or resolves to) a directory
     */
    static boolean didSelectDirectory(FilePicker picker, KeyPressMessage keyMsg) {
        if (!Binding.matches(keyMsg, picker.keyMap().open())) {
            return false;
        }

        List<DirEntry> files = picker.files();
        if (files.isEmpty()) {
            return false;
        }

        DirEntry f = files.get(picker.window().selected());
        try {
            return resolvesToDirectory(picker, f);
        } catch (IOException e) {
            logger.fine(() -> "Failed to resolve symlink for " + f.name());
            return false;
        }
    }

    /**
     * Resolves the entry's directory status, following symlink targets like
     * upstream's {@code filepath.EvalSymlinks}/{@code os.Stat} branch.
     * Upstream: https://github.com/charmbracelet/bubbles/blob/v0.21.0/filepicker/filepicker.go
     *
     * @param picker model providing the current directory
     * @param f      entry to classify
     * @return true when the entry is a directory or a symlink to one
     * @throws IOException when the symlink target cannot be read
     */
    static boolean resolvesToDirectory(FilePicker picker, DirEntry f) throws IOException {
        if (!f.isSymlink()) {
            return f.isDir();
        }
        return Files.readAttributes(
            Paths.get(picker.currentDirectory(), f.name()), BasicFileAttributes.class
        ).isDirectory();
    }
}
