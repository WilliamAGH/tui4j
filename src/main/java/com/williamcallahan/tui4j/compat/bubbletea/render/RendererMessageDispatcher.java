package com.williamcallahan.tui4j.compat.bubbletea.render;

import com.williamcallahan.tui4j.compat.bubbletea.DisableMouseMessage;
import com.williamcallahan.tui4j.compat.bubbletea.EnableMouseAllMotionMessage;
import com.williamcallahan.tui4j.compat.bubbletea.EnableMouseCellMotionMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.MessageShim;
import com.williamcallahan.tui4j.compat.bubbletea.PrintLineMessage;
import com.williamcallahan.tui4j.compat.bubbletea.SetWindowTitleMessage;
import com.williamcallahan.tui4j.compat.bubbletea.WindowSizeMessage;
import com.williamcallahan.tui4j.message.CopyToClipboardMessage;
import com.williamcallahan.tui4j.message.ResetMouseCursorMessage;
import com.williamcallahan.tui4j.message.SetMouseCursorPointerMessage;
import com.williamcallahan.tui4j.message.SetMouseCursorTextMessage;

/**
 * Routes renderer-bound messages to the appropriate handler methods.
 * <p>
 * Upstream: bubbletea/standard_renderer.go handleMessages
 */
final class RendererMessageDispatcher {

    /** Prevents instantiation; all methods are static. */
    private RendererMessageDispatcher() {
    }

    /**
     * Dispatches a message to the appropriate renderer or flush method.
     * <p>
     * Returns a {@link WindowSizeMessage} when the caller must update
     * its own width/height state; returns {@code null} otherwise.
     *
     * @param msg      incoming message
     * @param renderer renderer to invoke methods on
     * @param flush    flush pipeline for buffer-related operations
     * @return window size message for caller handling, or null
     */
    static WindowSizeMessage dispatch(Message msg, Renderer renderer, RendererFlush flush) {
        Message resolved = (msg instanceof MessageShim shim) ? shim.toMessage() : msg;

        if (resolved instanceof PrintLineMessage m) {
            flush.queuePrintLine(m.messageBody());
        } else if (resolved instanceof SetWindowTitleMessage m) {
            flush.setWindowTitle(m.title());
        } else if (resolved instanceof EnableMouseCellMotionMessage) {
            renderer.enableMouseCellMotion();
            renderer.enableMouseSGRMode();
        } else if (resolved instanceof EnableMouseAllMotionMessage) {
            renderer.enableMouseAllMotion();
            renderer.enableMouseSGRMode();
        } else if (resolved instanceof DisableMouseMessage) {
            renderer.disableMouseSGRMode();
            renderer.disableMouseNormalTracking();
            renderer.disableMouseCellMotion();
            renderer.disableMouseAllMotion();
        } else if (resolved instanceof SetMouseCursorTextMessage) {
            renderer.setMouseCursorText();
        } else if (resolved instanceof SetMouseCursorPointerMessage) {
            renderer.setMouseCursorPointer();
        } else if (resolved instanceof ResetMouseCursorMessage) {
            renderer.resetMouseCursor();
        } else if (resolved instanceof CopyToClipboardMessage(String text)) {
            renderer.copyToClipboard(text);
        } else if (resolved instanceof WindowSizeMessage wsm) {
            return wsm;
        }
        return null;
    }
}
