package com.medinote.medinotebackend.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String priority,
        String title,
        String body,
        String actionRoute,
        String metadataJson,
        boolean read,
        Instant readAt,
        Instant createdAt
) {}
