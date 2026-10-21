package com.williamcallahan.tui4j.term;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the clipboard guard the build relies on: the test JVM runs with
 * {@code tui4j.clipboard.disabled=true} so suites never touch the real system clipboard.
 */
class ClipboardTest {

    @Test
    @DisplayName("tryPaste reports nothing when clipboard access is disabled")
    void test_TryPasteReturnsNullWhenDisabled() {
        assertThat(Boolean.getBoolean("tui4j.clipboard.disabled")).isTrue();
        assertThat(Clipboard.tryPaste()).isNull();
    }

    @Test
    @DisplayName("tryCopy reports failure when clipboard access is disabled")
    void test_TryCopyReturnsFalseWhenDisabled() {
        assertThat(Clipboard.tryCopy("tui4j-clipboard-probe")).isFalse();
    }
}
