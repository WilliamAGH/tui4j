package com.williamcallahan.tui4j.duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests Go duration formatting.
 * <p>
 * Expected values are the output of Go's {@code time.Duration.String()}
 * (go1.26, {@code go/src/time/format.go}).
 * tui4j: src/test/java/com/williamcallahan/tui4j/duration/GoDurationTest.java
 */
class GoDurationTest {

    @ParameterizedTest(name = "{0}ns -> {1}")
    @MethodSource("durationCases")
    void testFormatMatchesGoDurationString(long nanos, String expected) {
        assertThat(GoDuration.format(Duration.ofNanos(nanos))).isEqualTo(expected);
    }

    /**
     * Proves sub-second values keep every significant digit, unlike a
     * fixed-precision rendering that rounds long millisecond values.
     */
    @Test
    void testSubSecondKeepsFullPrecision() {
        assertThat(GoDuration.format(Duration.ofNanos(1_234_567)))
            .isEqualTo("1.234567ms");
    }

    /**
     * Proves zero-valued intermediate units are printed, so hours never render
     * as an ambiguous "1h1s".
     */
    @Test
    void testZeroMinutesBetweenHoursAndSecondsArePrinted() {
        assertThat(GoDuration.format(Duration.ofSeconds(3_601)))
            .isEqualTo("1h0m1s");
        assertThat(GoDuration.format(Duration.ofNanos(3_600_123_000_000L)))
            .isEqualTo("1h0m0.123s");
    }

    private static Stream<Arguments> durationCases() {
        return Stream.of(
                Arguments.of(0L, "0s"),
                Arguments.of(1L, "1ns"),
                Arguments.of(999L, "999ns"),
                Arguments.of(1_000L, "1\u00B5s"),
                Arguments.of(1_500L, "1.5\u00B5s"),
                Arguments.of(100_000L, "100\u00B5s"),
                Arguments.of(1_000_000L, "1ms"),
                Arguments.of(1_500_000L, "1.5ms"),
                Arguments.of(100_000_000L, "100ms"),
                Arguments.of(1_000_000_000L, "1s"),
                Arguments.of(1_500_000_000L, "1.5s"),
                Arguments.of(59_000_000_000L, "59s"),
                Arguments.of(60_000_000_000L, "1m0s"),
                Arguments.of(61_000_000_000L, "1m1s"),
                Arguments.of(90_000_000_000L, "1m30s"),
                Arguments.of(3_599_000_000_000L, "59m59s"),
                Arguments.of(3_600_000_000_000L, "1h0m0s"),
                Arguments.of(3_661_000_000_000L, "1h1m1s"),
                Arguments.of(5_445_000_000_000L, "1h30m45s"),
                Arguments.of(36_330_000_000_000L, "10h5m30s"),
                Arguments.of(90_001_000_000_000L, "25h0m1s"),
                Arguments.of(-1_000_000_000L, "-1s"),
                Arguments.of(-1_000_000L, "-1ms"),
                Arguments.of(-3_600_000_000_000L, "-1h0m0s")
        );
    }
}
