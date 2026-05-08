package com.medinote.medinotebackend.scoring.dto;

import com.medinote.medinotebackend.user.Role;

import java.time.Instant;

public record ScoreCardResponse(
        Long userId,
        String email,
        String fullName,
        Role role,
        boolean enabled,
        double overallScore,
        double relevanceScore,
        double frequencyScore,
        double coverageScore,
        int totalSessions,
        int totalChatQuestions,
        int activeDays30,
        String featuresUsed,
        Instant lastSeenAt,
        Instant updatedAt,
        String rationale
) {}
