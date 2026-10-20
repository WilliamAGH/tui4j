package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbles.filepicker.FilePicker.DirEntry;
import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.bubbletea.ErrorMessage;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Key-driven navigation for the file picker.
 * <p>
 * Port of the {@code tea.KeyMsg} branch of {@code Model.Update} in
 * github.com/charmbracelet/bubbles/filepicker/filepicker.go: cursor and
 * window (min/max) movement, directory descent/ascend with view-history
 * stacks, and selection passthrough.
 */
final class FilePickerKeys {

    private FilePickerKeys() {
    }

    /**
     * Applies a key press to the picker's navigation state.
     *
     * @param picker model to mutate
     * @param keyMsg key press to handle
     * @return updated model and optional read command
     */
    static UpdateResult<FilePicker> handle(FilePicker picker, KeyPressMessage keyMsg) {
        KeyMap keyMap = picker.keyMap();

        if (handleNavigation(picker, keyMsg)) {
            return UpdateResult.from(picker);
        } else if (Binding.matches(keyMsg, keyMap.back())) {
            return handleBack(picker);
        } else if (Binding.matches(keyMsg, keyMap.open())) {
            return handleOpen(picker, keyMsg);
        } else if (
            Binding.matches(keyMsg, keyMap.select()) &&
            !Binding.matches(keyMsg, keyMap.open())
        ) {
            FilePickerSelection.didSelectFile(picker, keyMsg);
            return UpdateResult.from(picker);
        }
        return UpdateResult.from(picker);
    }

    /**
     * Ascends to the parent directory, restoring the pushed view when the
     * history stack is non-empty.
     *
     * @param picker model to mutate
     * @return updated model and read command
     */
    private static UpdateResult<FilePicker> handleBack(FilePicker picker) {
        Path current = Path.of(picker.currentDirectory());
        Path parent = current.getParent();
        if (parent != null) {
            picker.currentDirectory(parent.toString());
        } else if (current.isAbsolute() && current.getRoot() != null) {
            // At filesystem root; stay put (matches Go filepath.Dir behavior)
            picker.currentDirectory(current.getRoot().toString());
        }
        // Relative path with no parent stays unchanged
        if (picker.window().selectedStack().length() > 0) {
            picker.window().selected(picker.window().selectedStack().pop());
            picker.window().min(picker.window().minStack().pop());
            picker.window().max(picker.window().maxStack().pop());
        } else {
            picker.window().selected(0);
            picker.window().min(0);
            picker.window().max(picker.window().computeMaxIndex());
        }
        return UpdateResult.from(
            picker,
            DirectoryScanner.readDir(picker.id(), picker.currentDirectory(), picker.showHidden())
        );
    }

    /**
     * Applies cursor and window movement bindings.
     *
     * @param picker model to mutate
     * @param keyMsg key press to inspect
     * @return true when a navigation binding matched
     */
    private static boolean handleNavigation(FilePicker picker, KeyPressMessage keyMsg) {
        KeyMap keyMap = picker.keyMap();
        int fileCount = picker.files().size();

        if (Binding.matches(keyMsg, keyMap.goToTop())) {
            picker.window().selected(0);
            picker.window().min(0);
            picker.window().max(picker.window().computeMaxIndex());
            return true;
        } else if (Binding.matches(keyMsg, keyMap.goToLast())) {
            int lastIndex = Math.max(0, fileCount - 1);
            picker.window().selected(lastIndex);
            picker.window().max(lastIndex);
            picker.window().min(
                Math.min(Math.max(0, fileCount - picker.height()), lastIndex)
            );
            return true;
        } else if (Binding.matches(keyMsg, keyMap.down())) {
            if (fileCount == 0) {
                return true;
            }
            picker.window().selected(picker.window().selected() + 1);
            if (picker.window().selected() >= fileCount) {
                picker.window().selected(fileCount - 1);
            }
            if (picker.window().selected() > picker.window().max()) {
                picker.window().min(picker.window().min() + 1);
                picker.window().max(picker.window().max() + 1);
            }
            return true;
        } else if (Binding.matches(keyMsg, keyMap.up())) {
            picker.window().selected(picker.window().selected() - 1);
            if (picker.window().selected() < 0) {
                picker.window().selected(0);
            }
            if (picker.window().selected() < picker.window().min()) {
                picker.window().min(picker.window().min() - 1);
                picker.window().max(picker.window().max() - 1);
            }
            return true;
        } else if (Binding.matches(keyMsg, keyMap.pageDown())) {
            if (fileCount == 0) {
                return true;
            }
            picker.window().selected(picker.window().selected() + picker.height());
            if (picker.window().selected() >= fileCount) {
                picker.window().selected(Math.max(0, fileCount - 1));
            }
            picker.window().min(picker.window().min() + picker.height());
            picker.window().max(picker.window().max() + picker.height());

            if (picker.window().max() >= fileCount) {
                picker.window().max(Math.max(0, fileCount - 1));
                picker.window().min(
                    Math.min(Math.max(0, picker.window().max() - picker.height() + 1), picker.window().max())
                );
            }
            return true;
        } else if (Binding.matches(keyMsg, keyMap.pageUp())) {
            if (fileCount == 0) {
                return true;
            }
            picker.window().selected(picker.window().selected() - picker.height());
            if (picker.window().selected() < 0) {
                picker.window().selected(0);
            }
            picker.window().min(picker.window().min() - picker.height());
            picker.window().max(picker.window().max() - picker.height());

            if (picker.window().min() < 0) {
                picker.window().min(0);
                picker.window().max(
                    Math.min(fileCount - 1, picker.window().min() + picker.height() - 1)
                );
            }
            return true;
        }
        return false;
    }

    /**
     * Opens the selected entry: descends into directories (pushing the
     * current view) and records selection for allowed files.
     *
     * @param picker model to mutate
     * @param keyMsg key press to inspect
     * @return updated model and optional read command
     */
    private static UpdateResult<FilePicker> handleOpen(
        FilePicker picker,
        KeyPressMessage keyMsg
    ) {
        if (picker.files().isEmpty()) {
            return UpdateResult.from(picker);
        }

        DirEntry f = picker.files().get(picker.window().selected());
        boolean isDir = f.isDir();

        if (f.isSymlink()) {
            try {
                Path linkPath = Paths.get(picker.currentDirectory(), f.name());
                Path symlinkTarget = Files.readSymbolicLink(linkPath);
                Path resolved = linkPath
                    .getParent()
                    .resolve(symlinkTarget)
                    .normalize();
                isDir = Files.isDirectory(resolved);
            } catch (IOException e) {
                return UpdateResult.from(picker, () -> new ErrorMessage(e));
            }
        }

        if (
            (!isDir && picker.fileAllowed()) ||
            (isDir && picker.dirAllowed())
        ) {
            if (Binding.matches(keyMsg, picker.keyMap().select())) {
                picker.path(
                    Paths.get(picker.currentDirectory(), f.name()).toString()
                );
            }
        }

        if (!isDir) {
            return UpdateResult.from(picker);
        }

        picker.currentDirectory(
            Paths.get(picker.currentDirectory(), f.name()).toString()
        );
        picker.window().selectedStack().push(picker.window().selected());
        picker.window().minStack().push(picker.window().min());
        picker.window().maxStack().push(picker.window().max());
        picker.window().selected(0);
        picker.window().min(0);
        picker.window().max(picker.window().computeMaxIndex());
        return UpdateResult.from(
            picker,
            DirectoryScanner.readDir(picker.id(), picker.currentDirectory(), picker.showHidden())
        );
    }
}
