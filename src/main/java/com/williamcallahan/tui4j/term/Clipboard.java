package com.williamcallahan.tui4j.term;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for copying to and reading from the system clipboard via AWT or CLI tools.
 * <p>
 * This is a tui4j extension. Provides best-effort
 * clipboard access for local terminal sessions, complementing OSC 52 sequences
 * for remote/SSH terminals. Thread-safe for concurrent use.
 *
 * @see com.williamcallahan.tui4j.compat.bubbletea.render.StandardRenderer#copyToClipboard
 */
public final class Clipboard {

    private static final Logger LOG = Logger.getLogger(
        Clipboard.class.getName()
    );

    /** Prevents instantiation of this utility class. */
    private Clipboard() {}

    /**
     * Attempts to copy text to the clipboard.
     * <p>
     * Tries the local system clipboard first (AWT/CLI).
     * <p>
     * Can be disabled by setting system property {@code tui4j.clipboard.disabled=true},
     * useful for testing to avoid side effects on the real system clipboard.
     *
     * @param content the text to copy
     * @return true if copied via local mechanism, false otherwise
     */
    public static boolean tryCopy(String content) {
        if (Boolean.getBoolean("tui4j.clipboard.disabled")) {
            return false;
        }
        if (tryLocalClipboard(content)) {
            return true;
        }
        return copyViaCommand(content);
    }

    /**
     * Attempts to read text from the system clipboard.
     * <p>
     * Tries the local system clipboard first (AWT/CLI), mirroring how upstream bubbles reads the
     * clipboard for its paste commands. Can be disabled with system property
     * {@code tui4j.clipboard.disabled=true}, which tests set to keep suites off the real
     * system clipboard.
     *
     * @return the clipboard text, or {@code null} when unavailable, disabled, or the read failed
     */
    public static String tryPaste() {
        if (Boolean.getBoolean("tui4j.clipboard.disabled")) {
            return null;
        }
        String local = tryLocalClipboardPaste();
        if (local != null) {
            return local;
        }
        return pasteViaCommand();
    }

    /**
     * Attempts to read the clipboard through the AWT system clipboard.
     *
     * @return the clipboard text, or {@code null} when headless, unavailable, or not text
     */
    private static String tryLocalClipboardPaste() {
        try {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                Object content = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
                if (content instanceof String text) {
                    return text;
                }
            }
        } catch (Throwable ex) {
            // AWT failed (headless or other issue), fall through to CLI
            LOG.log(Level.FINE, "AWT clipboard read failed", ex);
        }
        return null;
    }

    /**
     * Attempts to read the clipboard via platform-specific CLI commands.
     * <p>
     * Uses pbpaste (macOS), Get-Clipboard (Windows), or xclip/xsel (Linux).
     *
     * @return the clipboard text, or {@code null} when no command produced text
     */
    private static String pasteViaCommand() {
        String osName = System.getProperty("os.name");
        String os = (osName != null) ? osName.toLowerCase() : "";

        if (os.contains("mac")) {
            return readProcess(new ProcessBuilder("pbpaste"));
        }
        if (os.contains("win")) {
            return readProcess(new ProcessBuilder(
                "powershell", "-NoProfile", "-Command", "Get-Clipboard"));
        }

        // Linux/Unix: try xclip first
        String content = readProcess(new ProcessBuilder("xclip", "-selection", "clipboard", "-o"));
        if (content != null) {
            return content;
        }

        // Fallback for Linux: xsel
        if (os.contains("nux") || os.contains("nix")) {
            return readProcess(new ProcessBuilder("xsel", "--clipboard", "--output"));
        }
        return null;
    }

    /**
     * Reads a clipboard command's standard output.
     *
     * @param pb the process builder configured for the clipboard command
     * @return the captured text when the process exits successfully, otherwise {@code null}
     */
    private static String readProcess(ProcessBuilder pb) {
        try {
            Process p = pb.start();
            String content = new String(
                p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            if (p.exitValue() == 0) {
                return content;
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "CLI clipboard read failed", e);
        }
        return null;
    }

    /**
     * Attempts clipboard copy via AWT system clipboard.
     *
     * @param content the text to copy
     * @return true if AWT clipboard succeeded, false if headless or AWT failed
     */
    private static boolean tryLocalClipboard(String content) {
        try {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                StringSelection selection = new StringSelection(content);
                Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(selection, selection);
                return true;
            }
        } catch (Throwable ex) {
            // AWT failed (headless or other issue), fall through to CLI
            LOG.log(Level.FINE, "AWT clipboard access failed", ex);
        }
        return false;
    }

    /**
     * Attempts clipboard copy via platform-specific CLI commands.
     * <p>
     * Uses pbcopy (macOS), clip (Windows), or xclip/xsel (Linux).
     *
     * @param content the text to copy
     * @return true if CLI command succeeded, false otherwise
     */
    private static boolean copyViaCommand(String content) {
        String osName = System.getProperty("os.name");
        String os = (osName != null) ? osName.toLowerCase() : "";
        ProcessBuilder pb = null;

        if (os.contains("mac")) {
            pb = new ProcessBuilder("pbcopy");
        } else if (os.contains("win")) {
            pb = new ProcessBuilder("clip");
        } else {
            // Linux/Unix: try xclip first
            pb = new ProcessBuilder("xclip", "-selection", "clipboard");
        }

        if (tryProcess(pb, content)) {
            return true;
        }

        // Fallback for Linux: xsel
        if ((os.contains("nux") || os.contains("nix")) && !os.contains("mac")) {
            pb = new ProcessBuilder("xsel", "--clipboard", "--input");
            return tryProcess(pb, content);
        }

        return false;
    }

    /**
     * Writes content to a process's stdin and waits for completion.
     *
     * @param pb the process builder configured for the clipboard command
     * @param content the text to write to the process
     * @return true if process exited successfully (exit code 0), false otherwise
     */
    private static boolean tryProcess(ProcessBuilder pb, String content) {
        try {
            Process p = pb.start();
            try (
                BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                        p.getOutputStream(),
                        StandardCharsets.UTF_8
                    )
                )
            ) {
                writer.write(content);
            }
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            LOG.log(Level.FINE, "CLI clipboard process failed", e);
            return false;
        }
    }
}
