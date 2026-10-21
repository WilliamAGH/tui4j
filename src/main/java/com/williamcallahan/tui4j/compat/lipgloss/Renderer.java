package com.williamcallahan.tui4j.compat.lipgloss;

import com.williamcallahan.tui4j.ansi.TextWidth;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstraction for rendering and terminal mode control.
 * Upstream: lipgloss/renderer.go
 */
public class Renderer {

    static Renderer defaultRenderer = new Renderer(Output.defaultOutput());

    /**
     * Returns the default renderer instance.
     *
     * @return default renderer
     */
    public static Renderer defaultRenderer() {
        return defaultRenderer;
    }

    private final Lock renderLock = new ReentrantLock();
    private Output output;
    private ColorProfile colorProfile;
    private boolean explicitColorProfile;

    private boolean hasDarkBackground;
    private boolean hasDarkBackgroundSet;
    private boolean explicitBackgroundColor;

    /**
     * Creates a renderer with the provided output.
     *
     * @param output output configuration
     */
    public Renderer(Output output) {
        this.output = output;
    }

    /**
     * Creates a new style bound to this renderer.
     *
     * @return new style
     */
    public Style newStyle() {
        return new Style(this);
    }

    /**
     * Returns whether the background is dark.
     *
     * @return {@code true} when the background is dark
     */
    public boolean hasDarkBackground() {
        if (hasDarkBackgroundSet || explicitBackgroundColor) {
            return hasDarkBackground;
        }

        renderLock.lock();
        try {
            hasDarkBackground = output.hasDarkBackground();
            hasDarkBackgroundSet = true;
            return hasDarkBackground;
        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Returns the color profile for this renderer.
     *
     * @return color profile
     */
    public ColorProfile colorProfile() {
        if (!explicitColorProfile && colorProfile == null) {
            renderLock.lock();
            try {
                colorProfile = output.envColorProfile();
            } finally {
                renderLock.unlock();
            }
        }
        return colorProfile;
    }

    /**
     * Sets the color profile.
     *
     * @param colorProfile color profile
     */
    public void setColorProfile(ColorProfile colorProfile) {
        renderLock.lock();
        try {
            this.colorProfile = colorProfile;
            this.explicitColorProfile = true;
        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Sets the background darkness flag.
     *
     * @param hasDarkBackground dark background flag
     */
    public void setHasDarkBackground(boolean hasDarkBackground) {
        renderLock.lock();
        try {
            this.hasDarkBackground = hasDarkBackground;
            hasDarkBackgroundSet = true;
            explicitBackgroundColor = true;
        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Sets environment variables for SSH/remote session support.
     * Currently a stub; full implementation pending.
     *
     * @param environment environment variable list
     */
    public void setEnvironment(java.util.List<String> environment) {
        // TODO: Wire environment to Output for SSH/remote terminal detection
    }

    /**
     * Places input within the given width and height.
     *
     * @param width available width
     * @param height available height
     * @param hPos horizontal position
     * @param vPos vertical position
     * @param input content to place
     * @param options whitespace options
     * @return positioned content
     */
    public String place(int width, int height, Position hPos, Position vPos, String input, Whitespace.WhitespaceOption... options) {
        return placeVertical(height, vPos, placeHorizontal(width, hPos, input, options), options);
    }

    /**
     * Places input vertically within the given height.
     *
     * @param height available height
     * @param position vertical position
     * @param input content to place
     * @param options whitespace options
     * @return positioned content
     */
    public String placeVertical(int height, Position position, String input, Whitespace.WhitespaceOption... options) {
        int contentHeight = (int) (input.chars().filter(ch -> ch == '\n').count() + 1);
        int gap = height - contentHeight;

        if (gap <= 0) {
            return input;
        }

        Whitespace whitespace = Whitespace.newWhiteSpace(this, options);
        TextLines textLines = TextLines.fromText(input);
        String emptyLine = whitespace.render(textLines.widestLineLength());

        StringBuilder builder = new StringBuilder();
        if (position.equals(Position.Top)) {
            builder.append(input).append('\n');

            for (int i = 0; i < gap; i++) {
                builder.append(emptyLine);
                if (i < gap - 1) {
                    builder.append('\n');
                }
            }

        } else if (position.equals(Position.Bottom)) {
            builder.append((emptyLine + "\n").repeat(gap)).append(input);
        } else {
            int split = (int) Math.round(gap * position.value());
            int top = gap - split;
            int bottom = gap - top;

            builder.append((emptyLine + "\n").repeat(top)).append(input);

            for (int i = 0; i < bottom; i++) {
                builder.append('\n').append(emptyLine);
            }
        }
        return builder.toString();
    }

    /**
     * Places input horizontally within the given width.
     *
     * @param width available width
     * @param position horizontal position
     * @param input content to place
     * @param options whitespace options
     * @return positioned content
     */
    public String placeHorizontal(int width, Position position, String input, Whitespace.WhitespaceOption... options) {
        TextLines textLines = TextLines.fromText(input);
        int contentWidth = textLines.widestLineLength();
        int gap = width - contentWidth;

        if (gap <= 0) {
            return input;
        }

        Whitespace whitespace = Whitespace.newWhiteSpace(this, options);
        StringBuilder builder = new StringBuilder();

        String[] lines = textLines.lines();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            int shorty = Math.max(0, contentWidth - TextWidth.measureCellWidth(line));

            if (position.equals(Position.Left)) {
                builder.append(line).append(whitespace.render(gap + shorty));
            } else if (position.equals(Position.Right)) {
                builder.append(whitespace.render(gap + shorty)).append(line);
            } else {
                int totalGap = gap + shorty;

                int split = (int) Math.round(totalGap * position.value());
                int left = totalGap - split;
                int right = totalGap - left;

                builder
                        .append(whitespace.render(left))
                        .append(line)
                        .append(whitespace.render(right));
            }

            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }

        return builder.toString();
    }
}
