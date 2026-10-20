package com.williamcallahan.tui4j.duration;

import java.time.Duration;

/**
 * Renders a {@link Duration} exactly like Go's {@code time.Duration.String()}.
 * <p>
 * The timer and stopwatch ports render Go durations with {@code Duration.String()},
 * which Java's {@link Duration} does not reproduce: it prints ISO-8601 values and
 * omits zero-valued units (for example {@code 3601s} as {@code PT1H1S} instead of
 * {@code 1h0m1s}). Owning that translation here keeps both ports on one
 * implementation of the Go format contract.
 * <p>
 * Go reference: {@code go/src/time/format.go} ({@code Duration.String}).
 * tui4j: src/main/java/com/williamcallahan/tui4j/duration/GoDuration.java
 */
public final class GoDuration {

    private static final long NANOS_PER_MICROSECOND = 1_000L;
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private static final int FRACTION_PRECISION_SECONDS = 9;

    /**
     * Prevents instantiation.
     */
    private GoDuration() {
    }

    /**
     * Formats a duration in Go's {@code time.Duration.String()} style.
     * <p>
     * Values below one second use the largest fitting sub-second unit
     * ({@code ns}, {@code µs}, or {@code ms}); larger values print hours, minutes,
     * and seconds with the fractional seconds trimmed of trailing zeros. Units
     * between significant units are always printed, so one hour and one second
     * renders as {@code 1h0m1s}, matching Go.
     *
     * @param duration duration to render
     * @return Go-style duration text
     */
    public static String format(Duration duration) {
        long nanos = duration.toNanos();
        boolean negative = nanos < 0;
        long value = Math.abs(nanos);

        StringBuilder result = new StringBuilder();
        if (value < NANOS_PER_SECOND) {
            if (value == 0) {
                return "0s";
            }
            appendSubSecond(result, value);
        } else {
            appendSeconds(result, value);
        }

        if (negative) {
            result.insert(0, '-');
        }
        return result.toString();
    }

    /** Appends a sub-second value with its largest fitting unit. */
    private static void appendSubSecond(StringBuilder result, long nanos) {
        int precision;
        String unit;
        long scale;
        if (nanos < NANOS_PER_MICROSECOND) {
            precision = 0;
            unit = "ns";
            scale = 1L;
        } else if (nanos < NANOS_PER_MILLISECOND) {
            precision = 3;
            unit = "\u00B5s";
            scale = NANOS_PER_MICROSECOND;
        } else {
            precision = 6;
            unit = "ms";
            scale = NANOS_PER_MILLISECOND;
        }

        result.append(nanos / scale);
        appendFraction(result, nanos % scale, precision);
        result.append(unit);
    }

    /** Appends seconds, then the minute and hour units Go keeps when non-zero. */
    private static void appendSeconds(StringBuilder result, long nanos) {
        long seconds = nanos / NANOS_PER_SECOND;
        long fraction = nanos % NANOS_PER_SECOND;

        StringBuilder secondsPart = new StringBuilder();
        secondsPart.append(seconds % 60);
        appendFraction(secondsPart, fraction, FRACTION_PRECISION_SECONDS);
        secondsPart.append('s');

        long minutes = seconds / 60;
        if (minutes > 0) {
            secondsPart.insert(0, (minutes % 60) + "m");

            long hours = minutes / 60;
            if (hours > 0) {
                secondsPart.insert(0, hours + "h");
            }
        }

        result.append(secondsPart);
    }

    /**
     * Appends a fractional part as {@code .digits}, trimming trailing zeros.
     *
     * @param result builder to append to
     * @param fraction fractional value below the unit scale
     * @param precision scale exponent of the unit
     */
    private static void appendFraction(StringBuilder result, long fraction, int precision) {
        if (fraction == 0) {
            return;
        }

        String digits = Long.toString(fraction);
        digits = "0".repeat(precision - digits.length()) + digits;

        int end = digits.length();
        while (end > 0 && digits.charAt(end - 1) == '0') {
            end--;
        }
        result.append('.').append(digits, 0, end);
    }
}
