package com.medinote.medinotebackend.auth.reset;

public record ResetPasswordRequest(String token, String newPassword) {}
