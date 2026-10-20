package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.ErrorMessage;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File selection bubble.
 * <p>
 * Port of github.com/charmbracelet/bubbles/filepicker/filepicker.go.
 * Allows navigating the filesystem and selecting files or directories.
 * Directory scanning lives in {@link DirectoryScanner}, key navigation in
 * {@link FilePickerKeys}, rendering in {@link FilePickerView}, and selection
 * queries in {@link FilePickerSelection}.
 */
public class FilePicker implements Model {

    private static final String DEFAULT_CURSOR = ">";
    private static final Logger logger = Logger.getLogger(
        FilePicker.class.getName()
    );

    private final int id;
    private String path;
    private String currentDirectory;
    private List<String> allowedTypes;
    private KeyMap keyMap;
    private List<DirEntry> files;
    private boolean showPermissions;
    private boolean showSize;
    private boolean showHidden;
    private boolean dirAllowed;
    private boolean fileAllowed;
    private boolean autoHeight;
    private final FilePickerWindow window = new FilePickerWindow();
    private Styles styles;
    private String cursorChar;
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private List<String> readErrors;

    /**
     * Creates a file picker with default configuration.
     */
    public FilePicker() {
        this.id = generateId();
        this.currentDirectory = ".";
        this.cursorChar = DEFAULT_CURSOR;
        this.allowedTypes = new ArrayList<>();
        this.showPermissions = true;
        this.showSize = true;
        this.showHidden = false;
        this.dirAllowed = false;
        this.fileAllowed = true;
        this.autoHeight = true;
        this.keyMap = new KeyMap();
        this.styles = Styles.defaultStyles();
        this.files = new ArrayList<>();
        this.readErrors = new ArrayList<>();
    }

    /**
     * Returns a unique instance identifier.
     *
     * @return unique id for this picker
     */
    private int generateId() {
        return nextId.getAndIncrement();
    }

    /**
     * Returns a command to populate the file list from the current directory.
     * Respects showHidden rules.
     *
     * @return command that emits a read directory message
     */
    @Override
    public Command init() {
        return DirectoryScanner.readDir(this.id, this.currentDirectory, this.showHidden);
    }

    /**
     * Updates the picker with a message.
     *
     * @param msg message to handle
     * @return updated model and optional command
     */
    @Override
    public UpdateResult<FilePicker> update(Message msg) {
        if (msg instanceof KeyPressMessage keyMsg) {
            return FilePickerKeys.handle(this, keyMsg);
        } else if (msg instanceof ReadDirMessage readDirMsg) {
            if (readDirMsg.id() != this.id) {
                return UpdateResult.from(this);
            }
            this.files = readDirMsg.entries();
            this.readErrors = readDirMsg.errors();
            // Clamp selection indices to prevent out-of-bounds access when directory shrinks
            this.window.clamp(this.files.size());
            return UpdateResult.from(this);
        } else if (msg instanceof ErrorMessage errorMsg) {
            logger.log(
                Level.WARNING,
                "File picker failed to read directory",
                errorMsg.error()
            );
            return UpdateResult.from(this);
        }

        return UpdateResult.from(this);
    }

    /**
     * Sets the maximum visible rows for the picker.
     *
     * @param height height in rows
     */
    public void setHeight(int height) {
        this.window.setHeight(height);
    }

    /**
     * Sets terminal size for layout calculations.
     * Clamps selection indices to prevent desync after resize.
     *
     * @param width  terminal width in columns
     * @param height terminal height in rows
     */
    public void setTerminalSize(int width, int height) {
        this.window.setTerminalSize(this.files.size(), height, this.autoHeight);
    }

    /**
     * Renders the current view.
     *
     * @return rendered file picker view
     */
    @Override
    public String view() {
        return FilePickerView.render(this);
    }

    /**
     * Returns true when the file name matches allowed extensions.
     *
     * @param file file name to test
     * @return true if selection is allowed
     */
    boolean canSelect(String file) {
        if (this.allowedTypes.isEmpty()) {
            return true;
        }

        for (String ext : this.allowedTypes) {
            if (file.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the last selected path.
     *
     * @return selected path or {@code null} if none selected
     */
    public String selectedPath() {
        return this.path;
    }

    /**
     * Returns true when the message corresponds to selecting a file.
     *
     * @param msg message to inspect
     * @return true when a file was selected
     */
    public boolean didSelectFile(Message msg) {
        if (msg instanceof KeyPressMessage keyMsg) {
            return FilePickerSelection.didSelectFile(this, keyMsg);
        }
        return false;
    }

    /**
     * Returns true when the message corresponds to selecting a directory.
     *
     * @param msg message to inspect
     * @return true when a directory was selected
     */
    public boolean didSelectDirectory(Message msg) {
        if (msg instanceof KeyPressMessage keyMsg) {
            return FilePickerSelection.didSelectDirectory(this, keyMsg);
        }
        return false;
    }

    /**
     * Returns the current directory.
     *
     * @return current directory
     */
    public String currentDirectory() {
        return this.currentDirectory;
    }

    /**
     * Returns any errors encountered while reading the current directory.
     * Errors are cleared when a new directory is read.
     *
     * @return read error messages
     */
    public List<String> readErrors() {
        return List.copyOf(this.readErrors);
    }

    /**
     * Returns true if there were errors reading the current directory.
     *
     * @return true when read errors are present
     */
    public boolean hasReadErrors() {
        return !this.readErrors.isEmpty();
    }

    /**
     * Sets the current directory for the picker.
     *
     * @param directory directory to use
     */
    public void setCurrentDirectory(String directory) {
        this.currentDirectory = directory;
    }

    /**
     * Returns the allowed file extensions list.
     *
     * @return copy of allowed extensions
     */
    public List<String> allowedTypes() {
        return new ArrayList<>(this.allowedTypes);
    }

    /**
     * Sets the allowed file extensions.
     *
     * @param types file extensions to allow
     */
    public void setAllowedTypes(String... types) {
        this.allowedTypes = new ArrayList<>(Arrays.asList(types));
    }

    /**
     * Returns whether hidden files are shown.
     *
     * @return true when hidden files are shown
     */
    public boolean showHidden() {
        return this.showHidden;
    }

    /**
     * Sets whether hidden files are shown.
     *
     * @param showHidden true to show hidden files
     */
    public void setShowHidden(boolean showHidden) {
        this.showHidden = showHidden;
    }

    /**
     * Returns whether directories can be selected.
     *
     * @return true when directory selection is allowed
     */
    public boolean dirAllowed() {
        return this.dirAllowed;
    }

    /**
     * Sets whether directories can be selected.
     *
     * @param dirAllowed true to allow directory selection
     */
    public void setDirAllowed(boolean dirAllowed) {
        this.dirAllowed = dirAllowed;
    }

    /**
     * Returns whether files can be selected.
     *
     * @return true when file selection is allowed
     */
    public boolean fileAllowed() {
        return this.fileAllowed;
    }

    /**
     * Sets whether files can be selected.
     *
     * @param fileAllowed true to allow file selection
     */
    public void setFileAllowed(boolean fileAllowed) {
        this.fileAllowed = fileAllowed;
    }

    /**
     * Returns whether permissions are shown.
     *
     * @return true when permissions are shown
     */
    public boolean showPermissions() {
        return this.showPermissions;
    }

    /**
     * Sets whether permissions are shown.
     *
     * @param showPermissions true to show permissions
     */
    public void setShowPermissions(boolean showPermissions) {
        this.showPermissions = showPermissions;
    }

    /**
     * Returns whether sizes are shown.
     *
     * @return true when sizes are shown
     */
    public boolean showSize() {
        return this.showSize;
    }

    /**
     * Sets whether sizes are shown.
     *
     * @param showSize true to show sizes
     */
    public void setShowSize(boolean showSize) {
        this.showSize = showSize;
    }

    /**
     * Returns the current picker height.
     *
     * @return height in rows
     */
    public int height() {
        return this.window.height();
    }

    /**
     * Returns the active styles.
     *
     * @return styles configuration
     */
    public Styles styles() {
        return this.styles;
    }

    /**
     * Sets the styles used for rendering.
     *
     * @param styles styles configuration
     */
    public void setStyles(Styles styles) {
        this.styles = styles;
    }

    /**
     * Returns the current key bindings.
     *
     * @return key map
     */
    public KeyMap keyMap() {
        return this.keyMap;
    }

    /**
     * Sets the key bindings.
     *
     * @param keyMap key bindings to set
     */
    public void setKeyMap(KeyMap keyMap) {
        this.keyMap = keyMap;
    }

    /**
     * Returns the cursor character.
     *
     * @return cursor character
     */
    public String cursorChar() {
        return this.cursorChar;
    }

    /**
     * Sets the cursor character.
     *
     * @param cursorChar cursor character
     */
    public void setCursorChar(String cursorChar) {
        this.cursorChar = cursorChar;
    }

    /**
     * Returns the picker instance id used to scope read messages.
     *
     * @return picker id
     */
    int id() {
        return this.id;
    }

    /**
     * Returns the current directory listing.
     *
     * @return directory entries
     */
    List<DirEntry> files() {
        return this.files;
    }

    /**
     * Returns the cursor and visible-window state.
     *
     * @return picker window state
     */
    FilePickerWindow window() {
        return this.window;
    }

    /**
     * Records the selected path.
     *
     * @param path selected path
     */
    void path(String path) {
        this.path = path;
    }

    /**
     * Sets the current directory without other state changes.
     *
     * @param currentDirectory directory path
     */
    void currentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    /**
     * Port of the file picker directory entry model.
     * Upstream: github.com/charmbracelet/bubbles/filepicker/filepicker.go (dirEntry)
     *
     * @param name        entry name
     * @param isDir       whether the entry is a directory (lstat semantics)
     * @param isSymlink   whether the entry is a symlink
     * @param size        entry size in bytes (lstat semantics)
     * @param permissions entry {@code os.FileMode} label with type character
     */
    public record DirEntry(
        String name,
        boolean isDir,
        boolean isSymlink,
        long size,
        String permissions
    ) {}
}
