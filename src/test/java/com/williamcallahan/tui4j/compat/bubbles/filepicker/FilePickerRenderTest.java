package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks filepicker row rendering to the upstream bubbles format: Go
 * {@code os.FileMode.String()} permission labels (type character included,
 * lstat semantics for symlinks) and {@code humanize.Bytes}-derived size
 * labels rendered through the width-7 {@code FileSize} style.
 */
class FilePickerRenderTest {

    private FilePicker pickerAt(Path dir) {
        FilePicker picker = new FilePicker();
        picker.setCurrentDirectory(dir.toString());
        picker.setTerminalSize(80, 40);
        Command command = picker.init();
        Message message = command.execute();
        picker.update(message);
        return picker;
    }

    @Test
    void testRowPermissionsIncludeGoModeTypeChar(@TempDir Path dir) throws Exception {
        Assumptions.assumeTrue(
            Files.getFileStore(dir).supportsFileAttributeView("posix"),
            "POSIX file permissions are unavailable"
        );
        Files.setPosixFilePermissions(
            Files.createDirectory(dir.resolve("adir")),
            PosixFilePermissions.fromString("rwxr-xr-x")
        );
        Files.setPosixFilePermissions(
            Files.createFile(dir.resolve("afile.txt")),
            PosixFilePermissions.fromString("rw-r--r--")
        );
        Path link = createSymbolicLink(dir.resolve("alink"), Path.of("afile.txt"));
        // The link's own mode is platform-defined: rwxrwxrwx on Linux, umask-derived on macOS.
        String linkPermissions = PosixFilePermissions.toString(
            Files.getPosixFilePermissions(link, LinkOption.NOFOLLOW_LINKS)
        );

        String view = pickerAt(dir).view();

        assertThat(view)
            .contains("drwxr-xr-x")
            .contains("-rw-r--r--")
            .contains("l" + linkPermissions + " ");
    }

    @Test
    void testSizeUsesHumanizeBytes(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("tiny.bin"), new byte[8]);
        Files.write(dir.resolve("small.bin"), new byte[500]);
        Files.write(dir.resolve("mid.bin"), new byte[1500]);

        String view = pickerAt(dir).view();

        assertThat(view)
            .contains("8B")
            .contains("500B")
            .contains("1.5kB");
    }

    /**
     * Upstream renders the selected row size with
     * {@code fmt.Sprintf("%"+FileSize.GetWidth()+"s", size)}, so a 500-byte
     * file occupies the same width-7 column as unselected rows.
     */
    @Test
    void testSelectedRowPadsSizeToFileSizeWidth(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("only.bin"), new byte[500]);

        String view = pickerAt(dir).view();

        assertThat(view).contains("   500B");
    }

    /**
     * A nonpositive size-column width must leave the selected label unpadded.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void testSelectedRowWithNonpositiveSizeWidth(int width, @TempDir Path dir) throws Exception {
        Files.write(dir.resolve("only.bin"), new byte[500]);
        FilePicker picker = pickerAt(dir);
        picker.setShowPermissions(false);
        picker.styles().fileSize().width(width);

        assertThat(picker.view()).startsWith(">500B only.bin\n");
    }

    /** A dangling link must not write warning stack traces onto the TUI terminal. */
    @Test
    void testDanglingSymlinkDoesNotEmitWarnings(@TempDir Path dir) throws Exception {
        createSymbolicLink(dir.resolve("link"), Path.of("missing-target"));
        FilePicker picker = pickerAt(dir);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamHandler handler = new StreamHandler(output, new SimpleFormatter());
        Logger root = Logger.getLogger("");
        root.addHandler(handler);
        try {
            assertThat(picker.view()).contains("link").doesNotContain(" → ");
            KeyPressMessage enter = new KeyPressMessage(new Key(KeyType.keyCR));
            assertThat(picker.didSelectFile(enter)).isFalse();
            assertThat(picker.didSelectDirectory(enter)).isFalse();
            handler.flush();
            assertThat(output.toString(StandardCharsets.UTF_8)).isEmpty();
        } finally {
            root.removeHandler(handler);
            handler.close();
        }
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
