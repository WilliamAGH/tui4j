package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FilePickerTest {

    private Path tempDir;
    private FilePicker filePicker;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("filepicker-test");
        filePicker = new FilePicker();

        Files.createDirectories(tempDir.resolve("subdir"));
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.java"));
        Files.createFile(tempDir.resolve(".hidden"));

        filePicker.setCurrentDirectory(tempDir.toString());
    }

    @Test
    void testInitialState() {
        FilePicker freshPicker = new FilePicker();
        assertNotNull(freshPicker);
        assertEquals(".", freshPicker.currentDirectory());
        assertTrue(freshPicker.fileAllowed());
        assertFalse(freshPicker.dirAllowed());
        assertFalse(freshPicker.showHidden());
    }

    @Test
    void testSetAllowedTypes() {
        filePicker.setAllowedTypes(".txt", ".java");
        assertEquals(2, filePicker.allowedTypes().size());
        assertTrue(filePicker.allowedTypes().contains(".txt"));
        assertTrue(filePicker.allowedTypes().contains(".java"));
    }

    @Test
    void testSetShowHidden() {
        assertFalse(filePicker.showHidden());
        filePicker.setShowHidden(true);
        assertTrue(filePicker.showHidden());
    }

    @Test
    void testSetDirAllowed() {
        assertFalse(filePicker.dirAllowed());
        filePicker.setDirAllowed(true);
        assertTrue(filePicker.dirAllowed());
    }

    @Test
    void testSetFileAllowed() {
        assertTrue(filePicker.fileAllowed());
        filePicker.setFileAllowed(false);
        assertFalse(filePicker.fileAllowed());
    }

    @Test
    void testSetCurrentDirectory() {
        filePicker.setCurrentDirectory("/tmp");
        assertEquals("/tmp", filePicker.currentDirectory());
    }

    @Test
    void testInitReturnsReadDirCommand() {
        Command cmd = filePicker.init();
        assertNotNull(cmd);
        Message msg = cmd.execute();
        assertNotNull(msg);
    }

    @Test
    void testUpdateWithKeyPressUp() {
        Command cmd = filePicker.init();
        Message msg = cmd.execute();
        filePicker.update(msg);

        KeyPressMessage keyMsg = new KeyPressMessage(new Key(KeyType.KeyUp));
        UpdateResult<FilePicker> result = filePicker.update(keyMsg);
        assertNotNull(result);
    }

    @Test
    void testUpdateWithKeyPressDown() {
        Command cmd = filePicker.init();
        Message msg = cmd.execute();
        filePicker.update(msg);

        KeyPressMessage keyMsg = new KeyPressMessage(new Key(KeyType.KeyDown));
        UpdateResult<FilePicker> result = filePicker.update(keyMsg);
        assertNotNull(result);
    }

    @Test
    void testDidSelectFileReturnsFalseOnEnter() {
        KeyPressMessage keyMsg = new KeyPressMessage(new Key(KeyType.keyCR));
        assertFalse(filePicker.didSelectFile(keyMsg));
    }

    @Test
    void testSetTerminalSize() {
        filePicker.setTerminalSize(80, 24);
        assertEquals(19, filePicker.height());
    }

    @Test
    void testStyles() {
        Styles styles = filePicker.styles();
        assertNotNull(styles);
        assertNotNull(styles.cursor());
        assertNotNull(styles.directory());
        assertNotNull(styles.file());
    }

    @Test
    void testKeyMap() {
        KeyMap keyMap = filePicker.keyMap();
        assertNotNull(keyMap);
        assertNotNull(keyMap.up());
        assertNotNull(keyMap.down());
        assertNotNull(keyMap.back());
        assertNotNull(keyMap.open());
        assertNotNull(keyMap.select());
    }

    @Test
    void testCursorChar() {
        assertEquals(">", filePicker.cursorChar());
        filePicker.setCursorChar("*");
        assertEquals("*", filePicker.cursorChar());
    }

    @Test
    void testSelectedPathInitiallyNull() {
        FilePicker freshPicker = new FilePicker();
        assertNull(freshPicker.selectedPath());
    }

    @Test
    void testPermissionsAndSizeVisibility() {
        assertTrue(filePicker.showPermissions());
        assertTrue(filePicker.showSize());

        filePicker.setShowPermissions(false);
        filePicker.setShowSize(false);

        assertFalse(filePicker.showPermissions());
        assertFalse(filePicker.showSize());
    }

    /**
     * A failed target lookup must not be reported as a selectable file.
     */
    @Test
    void testDanglingSymlinkIsNotSelectedByQuery(@TempDir Path dir) throws Exception {
        createSymbolicLink(dir.resolve("link"), Path.of("missing-target"));
        filePicker.setCurrentDirectory(dir.toString());
        filePicker.update(filePicker.init().execute());
        KeyPressMessage enter = new KeyPressMessage(new Key(KeyType.keyCR));

        assertFalse(filePicker.didSelectFile(enter));
        assertFalse(filePicker.didSelectDirectory(enter));
        assertNull(filePicker.selectedPath());
    }

    /**
     * Enter must leave selection and navigation unchanged for a dangling link.
     */
    @Test
    void testDanglingSymlinkIsNotSelectedByEnter(@TempDir Path dir) throws Exception {
        createSymbolicLink(dir.resolve("link"), Path.of("missing-target"));
        filePicker.setCurrentDirectory(dir.toString());
        filePicker.update(filePicker.init().execute());

        UpdateResult<FilePicker> result = filePicker.update(new KeyPressMessage(new Key(KeyType.keyCR)));

        // Upstream breaks out on a failed os.Stat; an ErrorMessage would end the program.
        assertNull(result.command());
        assertNull(filePicker.selectedPath());
        assertEquals(dir.toString(), filePicker.currentDirectory());
    }

    /** Ensures disabled file types cannot be reported as a successful selection. */
    @Test
    void testSelectionRejectsDisallowedFileType(@TempDir Path dir) throws Exception {
        Files.createFile(dir.resolve("source.java"));
        filePicker.setCurrentDirectory(dir.toString());
        filePicker.setAllowedTypes(".txt");
        filePicker.update(filePicker.init().execute());
        KeyPressMessage enter = new KeyPressMessage(new Key(KeyType.keyCR));

        filePicker.update(enter);
        assertNull(filePicker.selectedPath());
        assertFalse(filePicker.didSelectFile(enter));
        assertNull(filePicker.selectedPath());

        filePicker.setAllowedTypes(".java");
        filePicker.update(enter);
        assertEquals(dir.resolve("source.java").toString(), filePicker.selectedPath());
        assertTrue(filePicker.didSelectFile(enter));
        assertEquals(dir.resolve("source.java").toString(), filePicker.selectedPath());
    }

    /** Aborts link-dependent tests when the filesystem or process cannot create links. */
    private static Path createSymbolicLink(Path link, Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException exception) {
            return Assumptions.abort("Symbolic links are unavailable: " + exception.getMessage());
        }
    }
}
