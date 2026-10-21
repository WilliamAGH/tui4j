package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbles.filepicker.FilePicker.DirEntry;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.ErrorMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Produces the file picker's directory-listing command.
 * <p>
 * Port of readDir in
 * github.com/charmbracelet/bubbles/filepicker/filepicker.go: entries are
 * statted with lstat semantics (matching Go's {@code os.DirEntry.Info()}),
 * directories sort first then by name, and dot-prefixed names are dropped
 * unless hidden entries are requested.
 */
final class DirectoryScanner {

    private static final Logger logger = Logger.getLogger(DirectoryScanner.class.getName());

    private DirectoryScanner() {
    }

    /**
     * Builds a command that reads {@code directory} and emits a
     * {@link ReadDirMessage} tagged with the picker id.
     *
     * @param id         owning picker id
     * @param directory  directory to read
     * @param showHidden whether to include dot-prefixed entries
     * @return read command
     */
    static Command readDir(int id, String directory, boolean showHidden) {
        return () -> {
            try {
                List<DirEntry> entries = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                try (var stream = Files.list(Paths.get(directory))) {
                    stream.forEach(path -> scanEntry(path, showHidden, entries, errors));
                }
                entries.sort(
                    Comparator.comparing(DirEntry::isDir)
                        .reversed()
                        .thenComparing(DirEntry::name)
                );
                return new ReadDirMessage(id, entries, errors);
            } catch (IOException e) {
                return new ErrorMessage(e);
            }
        };
    }

    /**
     * Stats one directory child and appends it to {@code entries}, or
     * records a per-entry error label.
     *
     * @param path       child path
     * @param showHidden whether to include dot-prefixed entries
     * @param entries    accumulator for scanned entries
     * @param errors     accumulator for per-entry error labels
     */
    private static void scanEntry(
        Path path,
        boolean showHidden,
        List<DirEntry> entries,
        List<String> errors
    ) {
        String name = path.getFileName().toString();
        if (!showHidden && name.startsWith(".")) {
            return;
        }
        try {
            entries.add(entryFor(path, name));
        } catch (IOException | SecurityException e) {
            errors.add("Failed to read: " + name + " (" + e.getMessage() + ")");
            logger.log(Level.WARNING, "Failed to read entry " + path, e);
        }
    }

    /**
     * Builds one entry using lstat attributes, matching Go's
     * {@code os.DirEntry.Info()} semantics.
     *
     * @param path entry path
     * @param name entry file name
     * @return scanned entry
     * @throws IOException when attributes cannot be read
     */
    private static DirEntry entryFor(Path path, String name) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(
            path,
            BasicFileAttributes.class,
            LinkOption.NOFOLLOW_LINKS
        );
        return new DirEntry(
            name,
            attrs.isDirectory(),
            attrs.isSymbolicLink(),
            attrs.size(),
            modeString(path, attrs)
        );
    }

    /**
     * Renders the Go {@code os.FileMode.String()} label: a type character
     * ({@code d}, {@code l}, or {@code -}) followed by the permission bits.
     *
     * @param path  entry path
     * @param attrs lstat attributes already fetched for the entry
     * @return ten-character mode label, or a short Windows fallback label
     */
    private static String modeString(Path path, BasicFileAttributes attrs)
        throws IOException {
        char type = attrs.isSymbolicLink()
            ? 'l'
            : (attrs.isDirectory() ? 'd' : '-');
        try {
            return type + PosixFilePermissions.toString(
                Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
            );
        } catch (UnsupportedOperationException e) {
            return type + windowsPermissions(path);
        }
    }

    /**
     * Fallback permission label for non-POSIX filesystems.
     *
     * @param path entry path
     * @return best-effort permission label
     */
    private static String windowsPermissions(Path path) {
        return (Files.isReadable(path) ? "r" : "-")
            + (Files.isWritable(path) ? "w" : "-")
            + (Files.isExecutable(path) ? "x" : "-");
    }
}
