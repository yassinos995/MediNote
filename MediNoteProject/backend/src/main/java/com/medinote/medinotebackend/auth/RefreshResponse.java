package com.medinote.medinotebackend.auth;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) {}
