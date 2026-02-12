package com.medinote.medinotebackend.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String role,
        String email
) {}
