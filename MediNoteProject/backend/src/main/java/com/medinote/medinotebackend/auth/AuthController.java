package com.medinote.medinotebackend.auth;

import com.medinote.medinotebackend.auth.reset.ForgotPasswordRequest;
import com.medinote.medinotebackend.auth.reset.PasswordResetService;
import com.medinote.medinotebackend.auth.reset.ResetPasswordRequest;
import com.medinote.medinotebackend.security.JwtService;
import com.medinote.medinotebackend.security.RefreshTokenService;
import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          UserRepository userRepo,
                          RefreshTokenService refreshTokenService,
                          PasswordResetService passwordResetService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        User user = userRepo.findByEmail(req.email()).orElseThrow();

        String accessToken = jwtService.generateToken(user.getEmail());

        boolean remember = req.rememberMe() != null && req.rememberMe();
        String refreshToken = null;
        if (remember) {
            refreshToken = refreshTokenService.createRefreshToken(user, Duration.ofDays(30));
        }

        return ResponseEntity.ok(new LoginResponse(
                accessToken,
                refreshToken,
                user.getRole().name(),
                user.getEmail()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        if (req.refreshToken() == null || req.refreshToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("NO_REFRESH_TOKEN"));
        }

        User user = userRepo.findByEmail(req.email()).orElse(null);
        if (user == null || !refreshTokenService.isRefreshTokenValid(user, req.refreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_REFRESH_TOKEN"));
        }

        String newAccess   = jwtService.generateToken(user.getEmail());
        String newRefresh  = refreshTokenService.rotateRefreshToken(user, Duration.ofDays(30));

        return ResponseEntity.ok(new RefreshResponse(newAccess, newRefresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest req) {
        userRepo.findByEmail(req.email()).ifPresent(refreshTokenService::revoke);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        passwordResetService.requestReset(req.email());
        return ResponseEntity.ok("If the email exists, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        passwordResetService.confirmReset(req.token(), req.newPassword());
        return ResponseEntity.ok("Password updated successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("");

        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "role", role
        ));
    }

    // Exception handlers

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid email or password"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Account is disabled"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
