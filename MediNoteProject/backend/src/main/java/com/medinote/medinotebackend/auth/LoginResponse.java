package com.medinote.medinotebackend.auth;

public record LoginResponse(String accessToken, String role, String email) {
}
