package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that resizing preserves the selected file in the rendered window.
 */
class FilePickerResizeTest {

    /**
     * An explicit height shrink must keep the cursor and selected file visible.
     */
    @Test
    void selectedFileRemainsVisibleAfterHeightShrink(@TempDir Path directory) throws Exception {
        FilePicker picker = pickerWithSecondFileSelected(directory);

        picker.setHeight(1);

        assertThat(picker.view()).isEqualTo("> b.txt\n");
    }

    /**
     * An auto-height terminal shrink must keep the selected file visible.
     */
    @Test
    void selectedFileRemainsVisibleAfterTerminalShrink(@TempDir Path directory) throws Exception {
        FilePicker picker = pickerWithSecondFileSelected(directory);

        picker.setTerminalSize(80, 6);

        assertThat(picker.view()).isEqualTo("> b.txt\n");
    }

    /**
     * Loads two real files and selects the second through the public key API.
     */
    private FilePicker pickerWithSecondFileSelected(Path directory) throws Exception {
        Files.createFile(directory.resolve("a.txt"));
        Files.createFile(directory.resolve("b.txt"));
        FilePicker picker = new FilePicker();
        picker.setCurrentDirectory(directory.toString());
        picker.setShowPermissions(false);
        picker.setShowSize(false);
        picker.setTerminalSize(80, 7);
        picker.update(picker.init().execute());
        picker.update(new KeyPressMessage(new Key(KeyType.KeyDown)));
        assertThat(picker.view()).contains("> b.txt\n");
        return picker;
    }
}
