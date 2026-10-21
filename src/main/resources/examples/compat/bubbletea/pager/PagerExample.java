package examples.compat.bubbletea.pager;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.Program;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.bubbles.viewport.Viewport;
import com.williamcallahan.tui4j.ansi.TextWidth;
import com.williamcallahan.tui4j.compat.lipgloss.Borders;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.QuitMessage;
import com.williamcallahan.tui4j.compat.bubbletea.WindowSizeMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demonstrates a full-screen pager with header and footer.
 * Upstream: bubbletea/examples/pager/main.go
 */
public class PagerExample implements Model {

    private static final String TITLE = "Mr. Pager";

    private Viewport viewport;
    private boolean ready;
    private String content;
    private Style titleStyle;
    private Style infoStyle;

    /**
     * Builds a pager for the provided content.
     *
     * @param content text to display
     */
    public PagerExample(String content) {
        this.content = content;
        this.ready = false;
        this.viewport = Viewport.create(0, 0);

        Style border = Style.newStyle()
                .border(Borders.roundedBorder())
                .paddingTop(0)
                .paddingBottom(0)
                .paddingLeft(1)
                .paddingRight(1);

        this.titleStyle = border.copy();
        this.infoStyle = border.copy();
    }

    /**
     * Starts with no initial command for the pager.
     *
     * @return null to indicate no startup command
     */
    @Override
    public Command init() {
        return null;
    }

    /**
     * Handles resizing, quit keys, and forwards updates to the viewport.
     *
     * @param msg incoming message
     * @return next model state and command
     */
    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage windowSize) {
            int headerHeight = headerView().split("\n").length;
            int footerHeight = footerView().split("\n").length;
            int verticalMarginHeight = headerHeight + footerHeight;

            if (!ready) {
                viewport.setWidth(windowSize.width());
                viewport.setHeight(windowSize.height() - verticalMarginHeight);
                viewport.setYPosition(headerHeight);
                viewport.setContent(content);
                ready = true;
            } else {
                viewport.setWidth(windowSize.width());
                viewport.setHeight(windowSize.height() - verticalMarginHeight);
            }
        }

        if (msg instanceof KeyPressMessage keyPress) {
            String keyStr = keyPress.key();
            if ("ctrl+c".equals(keyStr) || "q".equals(keyStr) || "esc".equals(keyStr)) {
                return UpdateResult.from(this, QuitMessage::new);
            }
        }

        UpdateResult<Viewport> result = viewport.update(msg);
        return UpdateResult.from(this, result.command());
    }

    /**
     * Renders the header, viewport, and footer.
     *
     * @return rendered view
     */
    @Override
    public String view() {
        if (!ready) {
            return "\n  Initializing...";
        }
        return headerView() + "\n" + viewport.view() + "\n" + footerView();
    }

    /**
     * Builds the header with a title and horizontal rule.
     *
     * @return header text
     */
    private String headerView() {
        String title = titleStyle.render(TITLE);
        int lineLength = Math.max(0, viewport.getWidth() - stringWidth(title));
        String line = "─".repeat(lineLength);
        return joinHorizontal(title, line);
    }

    /**
     * Builds the footer with scroll percentage.
     *
     * @return footer text
     */
    private String footerView() {
        double percent = viewport.scrollPercent();
        String info = infoStyle.render(String.format("%.0f%%", percent * 100));
        int lineLength = Math.max(0, viewport.getWidth() - stringWidth(info));
        String line = "─".repeat(lineLength);
        return joinHorizontal(line, info);
    }

    /**
     * Measures the rendered cell width for layout.
     *
     * @param s string to measure
     * @return cell width
     */
    private int stringWidth(String s) {
        return TextWidth.measureCellWidth(s);
    }

    /**
     * Joins pieces without inserting separators.
     *
     * @param parts segments to join
     * @return concatenated string
     */
    private String joinHorizontal(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append("");
            }
        }
        return sb.toString();
    }

    /**
     * Runs the pager example with mouse scrolling enabled.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        String content = loadContent();
        if (content == null) {
            content = DEFAULT_CONTENT;
        }
        new Program(new PagerExample(content))
                .withAltScreen()
                .withMouseCellMotion()
                .run();
    }

    /**
     * Loads pager content from local files when available.
     *
     * @return content text or null if not found
     */
    private static String loadContent() {
        try {
            Path path = Paths.get("pager.md");
            if (Files.exists(path)) {
                return Files.readString(path);
            }
            path = Paths.get("examples/generic/pager/pager.md");
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (IOException e) {
            System.out.println("could not load file: " + e.getMessage());
        }
        return null;
    }

    private static final String DEFAULT_CONTENT = """
            # Mr. Pager

            This is an example of the Pager component from the Bubbles component library.

            ## Features

            - Scrollable content display
            - Line and page navigation
            - Scroll position indicator
            - Mouse wheel support
            - Full terminal size with alternate screen buffer

            ## Usage

            Use the following keys to navigate:
            - ↑ / ↓ : Scroll line by line
            - PgUp / PgDn : Scroll page by page
            - Home : Go to top
            - End : Go to bottom
            - q / ctrl+c / esc : Quit

            ## Mouse Support

            The pager also supports mouse wheel scrolling when enabled with
            `WithMouseCellMotion()` option.

            ## Lorem Ipsum

            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod
            tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
            quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
            consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
            cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat
            non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

            ## Another Section

            Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium
            doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore
            veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim
            ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia
            consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.

            ## Yet More Content

            Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur,
            adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et
            dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis
            nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid
            ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea
            voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem
            eum fugiat quo voluptas nulla pariatur?

            ## Final Section

            At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis
            praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias
            excepturi sint occaecati cupiditate non provident, similique sunt in culpa
            qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum
            quidem rerum facilis est et expedita distinctio. Nam libero tempore, cum soluta
            nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat
            facere possimus, omnis voluptas assumenda est, omnis dolor repellendus.

            ## The End

            Thank you for using Mr. Pager!
            """;
}
