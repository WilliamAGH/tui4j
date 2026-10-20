package com.williamcallahan.tui4j.compat.bubbles.filepicker;

import java.util.Locale;

/**
 * Binary-scaled byte labels matching the {@code humanize.Bytes} output that
 * upstream bubbles feeds through {@code strings.Replace(s, " ", "", 1)}.
 * <p>
 * Upstream: github.com/dustin/go-humanize {@code Bytes} (SI base 1000, one
 * decimal below 10, none at or above, space removed once).
 */
final class HumanBytes {

    private static final String[] SUFFIXES = {"B", "kB", "MB", "GB", "TB", "PB", "EB"};

    private HumanBytes() {
    }

    /**
     * Formats a byte count the way upstream renders file sizes.
     *
     * @param bytes byte count
     * @return compact size label such as {@code 8B}, {@code 500B}, {@code 1.5kB}
     */
    static String format(long bytes) {
        if (bytes < 10) {
            return bytes + "B";
        }

        int exponent = (int) Math.floor(Math.log(bytes) / Math.log(1000));
        exponent = Math.min(exponent, SUFFIXES.length - 1);

        double value = Math.floor(bytes / Math.pow(1000, exponent) * 10 + 0.5) / 10;
        String label = value < 10
            ? String.format(Locale.ROOT, "%.1f", value)
            : String.format(Locale.ROOT, "%.0f", value);

        return label + SUFFIXES[exponent];
    }
}
