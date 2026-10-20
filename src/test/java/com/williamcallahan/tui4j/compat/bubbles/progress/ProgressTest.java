package com.williamcallahan.tui4j.compat.bubbles.progress;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.compat.lipgloss.color.RGB;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProgressTest {

    /**
     * Registers terminal info so rendering tests do not depend on another test
     * class having initialized the shared provider first.
     */
    @BeforeEach
    void setUp() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
    }

    @Test
    void testDefaultValues() {
        Progress progress = new Progress();

        assertThat(progress.width()).isEqualTo(40);
        assertThat(progress.full()).isEqualTo('█');
        assertThat(progress.empty()).isEqualTo('░');
        assertThat(progress.fullColor()).isEqualTo("#7571F9");
        assertThat(progress.emptyColor()).isEqualTo("#606060");
        assertThat(progress.showPercentage()).isTrue();
        assertThat(progress.percent()).isEqualTo(0.0);
        assertThat(progress.percentShown()).isEqualTo(0.0);
        assertThat(progress.isAnimating()).isFalse();
    }

    @Test
    void testWithWidth() {
        Progress progress = new Progress().withWidth(80);
        assertThat(progress.width()).isEqualTo(80);
    }

    @Test
    void testWithFullCharacter() {
        Progress progress = new Progress().withFull('#');
        assertThat(progress.full()).isEqualTo('#');
    }

    @Test
    void testWithEmptyCharacter() {
        Progress progress = new Progress().withEmpty('-');
        assertThat(progress.empty()).isEqualTo('-');
    }

    @Test
    void testWithFullColor() {
        Progress progress = new Progress().withFullColor("#ff0000");
        assertThat(progress.fullColor()).isEqualTo("#ff0000");
    }

    @Test
    void testWithEmptyColor() {
        Progress progress = new Progress().withEmptyColor("#0000ff");
        assertThat(progress.emptyColor()).isEqualTo("#0000ff");
    }

    @Test
    void testWithoutPercentage() {
        Progress progress = new Progress().withoutPercentage();
        assertThat(progress.showPercentage()).isFalse();
    }

    @Test
    void testWithPercentFormat() {
        Progress progress = new Progress().withPercentFormat(" %5.1f%%");
        assertThat(progress.percentFormat()).isEqualTo(" %5.1f%%");
    }

    @Test
    void testSetPercentCommand() {
        Progress progress = new Progress();
        Command cmd = progress.setPercent(0.5);

        assertThat(progress.targetPercent()).isEqualTo(0.5);
        assertThat(cmd).isNotNull();
    }

    @Test
    void testSetPercentClampedToOne() {
        Progress progress = new Progress();
        progress.setPercent(1.5);

        assertThat(progress.targetPercent()).isEqualTo(1.0);
    }

    @Test
    void testSetPercentClampedToZero() {
        Progress progress = new Progress();
        progress.setPercent(-0.5);

        assertThat(progress.targetPercent()).isEqualTo(0.0);
    }

    @Test
    void testIncrPercent() {
        Progress progress = new Progress();
        progress.setPercent(0.5);
        Command cmd = progress.incrPercent(0.2);

        assertThat(progress.targetPercent()).isEqualTo(0.7);
        assertThat(cmd).isNotNull();
    }

    @Test
    void testDecrPercent() {
        Progress progress = new Progress();
        progress.setPercent(0.5);
        Command cmd = progress.decrPercent(0.2);

        assertThat(progress.targetPercent()).isEqualTo(0.3);
        assertThat(cmd).isNotNull();
    }

    @Test
    void testUpdateWithFrameMsg() {
        Progress progress = new Progress();
        progress.setPercent(0.5);
        int currentTag = progress.tag();

        FrameMessage frameMsg = new FrameMessage(progress.id(), currentTag);
        UpdateResult<Progress> result = progress.update(frameMsg);

        assertThat(result.model()).isEqualTo(progress);
        assertThat(result.command()).isNotNull();
    }

    @Test
    void testUpdateWithWrongIdIgnored() {
        Progress progress = new Progress();
        progress.setPercent(0.5);
        double beforePercentShown = progress.percentShown();

        FrameMessage frameMsg = new FrameMessage(-1, progress.tag());
        progress.update(frameMsg);

        assertThat(progress.percentShown()).isEqualTo(beforePercentShown);
    }

    @Test
    void testUpdateWithWrongTagIgnored() {
        Progress progress = new Progress();
        progress.setPercent(0.5);
        double beforePercentShown = progress.percentShown();

        FrameMessage frameMsg = new FrameMessage(progress.id(), -1);
        progress.update(frameMsg);

        assertThat(progress.percentShown()).isEqualTo(beforePercentShown);
    }

    @Test
    void testViewWithZeroPercent() {
        Progress progress = new Progress().withWidth(10).withoutPercentage();
        String view = progress.view();

        assertThat(view).isNotNull();
        assertThat(view.length()).isGreaterThanOrEqualTo(10);
        assertThat(view).contains("░");
    }

    @Test
    void testViewWithFullPercent() {
        Progress progress = new Progress().withWidth(10).withoutPercentage();
        progress.setPercent(1.0);

        for (int i = 0; i < 60; i++) {
            FrameMessage frameMsg = new FrameMessage(progress.id(), progress.tag());
            progress.update(frameMsg);
        }

        String view = progress.view();

        assertThat(view).isNotNull();
        assertThat(view.length()).isGreaterThanOrEqualTo(10);
        assertThat(view).contains("█");
    }

    @Test
    void testWithDefaultGradient() {
        Progress progress = new Progress().withDefaultGradient();

        assertThat(progress.percentShown()).isEqualTo(0.0);
    }

    @Test
    void testWithScaledGradient() {
        Progress progress = new Progress().withScaledGradient("#ff0000", "#00ff00");

        assertThat(progress.percentShown()).isEqualTo(0.0);
    }

    @Test
    void testIsAnimatingWhenAtTarget() {
        Progress progress = new Progress();
        progress.setPercent(0.5);

        while (progress.isAnimating()) {
            FrameMessage frameMsg = new FrameMessage(progress.id(), progress.tag());
            progress.update(frameMsg);
        }

        assertThat(progress.isAnimating()).isFalse();
    }

    @Test
    void testInitReturnsNull() {
        Progress progress = new Progress();
        assertThat(progress.init()).isNull();
    }

    @Test
    void testViewAsWithPercentage() {
        Progress progress = new Progress().withWidth(10).withShowPercentage(true);
        progress.setColorProfile(ColorProfile.ANSI256);

        String view = progress.viewAs(0.5);

        assertThat(view).contains("50%");
    }

    /**
     * A 16-color terminal must receive 16-color escapes, not the 256-color
     * approximation the bar used to emit regardless of the profile.
     */
    @Test
    void testAnsiProfileEmitsAnsiColorEscapes() {
        Progress progress = new Progress().withWidth(3).withoutPercentage();
        progress.setColorProfile(ColorProfile.ANSI);

        String filled = progress.viewAs(1.0);
        assertThat(filled).contains("\033[94m");
        assertThat(filled).doesNotContain("38;5;");

        String empty = progress.viewAs(0.0);
        assertThat(empty).contains("\033[90m");
        assertThat(empty).doesNotContain("38;5;");
    }

    /**
     * A 256-color terminal must show the palette entry the rest of the library
     * derives for the same color string, not a second quantization.
     */
    @Test
    void testAnsi256ColorsUseCanonicalQuantization() {
        Progress progress = new Progress().withWidth(10).withoutPercentage();
        progress.setColorProfile(ColorProfile.ANSI256);

        assertThat(progress.viewAs(1.0))
                .contains("\033[38;5;" + RGB.fromHexString("#7571F9").toANSI256Color().value() + "m");
        assertThat(progress.viewAs(0.0))
                .contains("\033[38;5;" + RGB.fromHexString("#606060").toANSI256Color().value() + "m");
    }

    @Test
    void testSetWidth() {
        Progress progress = new Progress();
        progress.setWidth(60);
        assertThat(progress.width()).isEqualTo(60);
    }

    /**
     * Gradient cells must follow upstream's CIELUV blend, not an sRGB one.
     * <p>
     * Expected cells are {@code go-colorful.Color.BlendLuv} rounded to bytes, the
     * colors bubbles interpolates for the ramp; an sRGB interpolation would put
     * olive {@code 128,128,0} in the middle of a red-to-green ramp. A cell can be
     * one byte above upstream's terminal sequence because termenv truncates the
     * channel after a hex round trip ({@code 164/255*255} is {@code 163.99…}) while
     * this port emits the parsed hex value.
     */
    @Test
    void testGradientBlendsInCieluv() {
        Progress progress = new Progress().withoutPercentage().withWidth(5)
                .withGradient("#FF0000", "#00FF00");
        progress.setColorProfile(ColorProfile.TrueColor);

        assertThat(cellColors(progress.viewAs(1.0))).containsExactly(
                "38;2;255;0;0", "38;2;243;110;0", "38;2;218;164;0", "38;2;172;211;0", "38;2;0;255;0");
    }

    /**
     * A full bar stretches the ramp across the whole bar while a half-full one
     * only stretches it across the filled cells when the ramp is scaled.
     */
    @Test
    void testGradientScalesToTheFilledCells() {
        Progress plain = new Progress().withoutPercentage().withWidth(10)
                .withGradient("#FF0000", "#00FF00");
        Progress scaled = new Progress().withoutPercentage().withWidth(10)
                .withScaledGradient("#FF0000", "#00FF00");
        plain.setColorProfile(ColorProfile.TrueColor);
        scaled.setColorProfile(ColorProfile.TrueColor);

        assertThat(cellColors(plain.viewAs(0.5))).startsWith(
                "38;2;255;0;0", "38;2;251;71;0", "38;2;245;103;0", "38;2;236;130;0", "38;2;225;153;0");
        assertThat(cellColors(scaled.viewAs(0.5))).startsWith(
                "38;2;255;0;0", "38;2;243;110;0", "38;2;218;164;0", "38;2;172;211;0", "38;2;0;255;0");
    }

    /**
     * The animation must follow upstream harmonica's closed-form spring, not a
     * naive Euler step: bubbles builds the spring with
     * {@code harmonica.NewSpring(harmonica.FPS(60), 18.0, 1.0)}, so the first
     * frame toward 1.0 lands at about 0.0369 (Euler overshoots to 0.09).
     */
    @Test
    void testFrameFollowsUpstreamHarmonicaTrajectory() {
        Progress progress = new Progress();
        progress.setPercent(1.0);

        progress.update(new FrameMessage(progress.id(), progress.tag()));

        assertThat(progress.percentShown()).isCloseTo(0.036936310446821219, within(1e-6));
    }

    /**
     * Upstream {@code IsAnimating} compares the signed velocity
     * ({@code dist < 0.001 && velocity < 0.01}), so an under-damped spring stops
     * when it crosses back within 0.001 of the target while moving downward,
     * after 43 frames at 1.000621; taking the absolute value keeps animating
     * until frame 47.
     */
    @Test
    void testUnderDampedStopsOnTheUpstreamFrame() {
        Progress progress = new Progress();
        progress.setSpringOptions(18.0, 0.5);
        progress.setPercent(1.0);

        int frames = 0;
        while (progress.isAnimating()) {
            progress.update(new FrameMessage(progress.id(), progress.tag()));
            frames++;
            assertThat(frames).isLessThan(1000);
        }

        assertThat(frames).isEqualTo(43);
        assertThat(progress.percentShown()).isCloseTo(1.0006214179649529, within(1e-6));
    }

    /**
     * Extracts the truecolor escape sequences of a rendered bar.
     *
     * @param view rendered progress bar
     * @return escape sequence prefixes in cell order
     */
    private static java.util.List<String> cellColors(String view) {
        java.util.List<String> colors = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("38;2;\\d+;\\d+;\\d+")
                .matcher(view);
        while (matcher.find()) {
            colors.add(matcher.group());
        }
        return colors;
    }
}
