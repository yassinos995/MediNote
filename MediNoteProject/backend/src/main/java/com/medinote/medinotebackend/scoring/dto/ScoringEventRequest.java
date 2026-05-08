package com.medinote.medinotebackend.scoring.dto;

import java.util.Map;

public record ScoringEventRequest(
        String eventType,
        String feature,
        String surface,
        Map<String, Object> metadata
) {}
