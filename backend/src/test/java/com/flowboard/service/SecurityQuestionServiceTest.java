package com.flowboard.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityQuestionServiceTest {
    private final SecurityQuestionService service = new SecurityQuestionService();

    @Test
    void normalizeAnswer_trimsLowercasesCollapsesWhitespaceAndStripsPunctuation() {
        assertEquals("john doe", service.normalizeAnswer("  John,   Doe! "));
        assertEquals("pt", service.normalizeAnswer("P@t"));
        assertEquals("a b c", service.normalizeAnswer("a\tb\nc"));
    }

    @Test
    void isLockedOut_requiresFailedAttemptThresholdAndRecentAttempt() {
        assertFalse(service.isLockedOut(2, LocalDateTime.now()));
        assertTrue(service.isLockedOut(3, LocalDateTime.now().minusMinutes(1)));
        assertFalse(service.isLockedOut(3, LocalDateTime.now().minusMinutes(16)));
    }
}
