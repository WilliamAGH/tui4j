package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

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
        Files.setPosixFilePermissions(
            Files.createDirectory(dir.resolve("adir")),
            PosixFilePermissions.fromString("rwxr-xr-x")
        );
        Files.setPosixFilePermissions(
            Files.createFile(dir.resolve("afile.txt")),
            PosixFilePermissions.fromString("rw-r--r--")
        );
        Files.createSymbolicLink(dir.resolve("alink"), Path.of("afile.txt"));

        String view = pickerAt(dir).view();

        assertThat(view)
            .contains("drwxr-xr-x")
            .contains("-rw-r--r--")
            .contains("lrwxrwxrwx");
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
}
