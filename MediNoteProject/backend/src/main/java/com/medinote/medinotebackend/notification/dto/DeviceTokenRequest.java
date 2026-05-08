package com.medinote.medinotebackend.notification.dto;

public record DeviceTokenRequest(
        String platform,
        String deviceId,
        String deviceToken
) {}
