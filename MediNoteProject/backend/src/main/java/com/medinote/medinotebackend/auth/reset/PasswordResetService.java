package com.medinote.medinotebackend.auth.reset;

import com.medinote.medinotebackend.mail.EmailService;
import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final EmailService emailService;

    @Value("${app.reset.base-url}")
    private String resetBaseUrl;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepo,
            UserRepository userRepo,
            PasswordEncoder encoder,
            EmailService emailService
    ) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.emailService = emailService;
    }

    @Transactional
    public void requestReset(String email) {
        userRepo.findByEmail(email).ifPresent(u -> {
            tokenRepo.deleteByEmail(email);

            String token = generateToken();
            Instant exp = Instant.now().plus(15, ChronoUnit.MINUTES);

            tokenRepo.save(new PasswordResetToken(email, token, exp));

            String link = resetBaseUrl + token;
            emailService.sendPasswordResetEmail(email, link);
        });
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (prt.isUsed()) throw new RuntimeException("Token already used");
        if (prt.getExpiresAt().isBefore(Instant.now())) throw new RuntimeException("Token expired");

        User user = userRepo.findByEmail(prt.getEmail()).orElseThrow();
        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);

        prt.setUsed(true);
        tokenRepo.save(prt);
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
