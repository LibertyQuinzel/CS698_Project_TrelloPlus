package com.flowboard.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityQuestionService {
    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final int LOCKOUT_MINUTES = 15;

    private static final List<String> SYSTEM_QUESTIONS = List.of(
        "Name a teacher or mentor who inspired you",
        "What was your first job?",
        "What was the name of your first pet?",
        "What city were you born in?",
        "What is your favorite book?",
        "What was the model of your first car?",
        "What was your childhood nickname?",
        "What is the name of the street you grew up on?",
        "What was the name of your elementary school?",
        "What is your favorite movie?",
        "What is your mother's maiden name?",
        "What was the first concert you attended?"
    );

    public List<String> getSystemQuestions() {
        return SYSTEM_QUESTIONS;
    }

    public String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }

        return answer.trim()
            .toLowerCase()
            .replaceAll("\\s+", " ")
            .replaceAll("[^a-z0-9 ]", "");
    }

    public boolean isLockedOut(int failedAttempts, LocalDateTime lastAttemptTime) {
        if (failedAttempts < MAX_FAILED_ATTEMPTS || lastAttemptTime == null) {
            return false;
        }

        return lastAttemptTime.plusMinutes(LOCKOUT_MINUTES).isAfter(LocalDateTime.now());
    }

    public LocalDateTime resetTokenExpiresAt() {
        return LocalDateTime.now().plusHours(1);
    }
}
