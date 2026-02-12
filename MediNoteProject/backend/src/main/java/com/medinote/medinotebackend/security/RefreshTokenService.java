package com.medinote.medinotebackend.security;

import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    // Create a new refresh token, store its HASH in DB, return raw token to client
    public String createRefreshToken(User user, Duration validity) {
        String raw = generateSecureToken();
        user.setRefreshTokenHash(encoder.encode(raw)); // store hash only
        user.setRefreshTokenExpiry(Instant.now().plus(validity));
        userRepo.save(user);
        return raw;
    }

    public boolean isRefreshTokenValid(User user, String rawToken) {
        if (user.getRefreshTokenHash() == null || user.getRefreshTokenExpiry() == null) return false;
        if (Instant.now().isAfter(user.getRefreshTokenExpiry())) return false;
        return encoder.matches(rawToken, user.getRefreshTokenHash());
    }

    // Rotate refresh token (recommended): old refresh becomes invalid immediately
    public String rotateRefreshToken(User user, Duration validity) {
        return createRefreshToken(user, validity);
    }

    public void revoke(User user) {
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiry(null);
        userRepo.save(user);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48]; // 48 bytes ~ 64 chars base64url
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
