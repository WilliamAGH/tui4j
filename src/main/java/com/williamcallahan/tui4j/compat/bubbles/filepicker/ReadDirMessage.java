package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbles.filepicker.FilePicker.DirEntry;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import java.util.List;

/**
 * Message carrying a directory listing back to the file picker.
 * <p>
 * Port of readDirMsg in
 * github.com/charmbracelet/bubbles/filepicker/filepicker.go.
 *
 * @param id      picker id this listing belongs to
 * @param entries sorted directory entries
 * @param errors  per-entry read error labels
 */
record ReadDirMessage(
    int id,
    List<DirEntry> entries,
    List<String> errors
) implements Message {
}
