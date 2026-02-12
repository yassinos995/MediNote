package com.medinote.medinotebackend.auth;

public record RefreshRequest(
        String email,
        String refreshToken
) {}
