package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbles.filepicker.FilePicker.DirEntry;
import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
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
        boolean isDir = resolvesToDirectory(picker, f);
        boolean selectable = (!isDir && picker.fileAllowed())
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
        return resolvesToDirectory(picker, f);
    }

    /**
     * Resolves the entry's directory status, following symlink targets like
     * upstream's {@code filepath.EvalSymlinks} branch.
     *
     * @param picker model providing the current directory
     * @param f      entry to classify
     * @return true when the entry is a directory or a symlink to one
     */
    static boolean resolvesToDirectory(FilePicker picker, DirEntry f) {
        if (f.isDir()) {
            return true;
        }
        if (!f.isSymlink()) {
            return false;
        }
        try {
            Path linkPath = Paths.get(picker.currentDirectory(), f.name());
            Path symlinkTarget = Files.readSymbolicLink(linkPath);
            Path resolved = linkPath.getParent().resolve(symlinkTarget).normalize();
            return Files.isDirectory(resolved);
        } catch (IOException e) {
            logger.log(
                Level.WARNING,
                "Failed to resolve symlink for " + f.name(),
                e
            );
            return false;
        }
    }
}
