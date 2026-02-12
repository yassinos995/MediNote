package com.medinote.medinotebackend.auth;

import com.medinote.medinotebackend.auth.reset.PasswordResetService;
import com.medinote.medinotebackend.security.JwtService;
import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import com.medinote.medinotebackend.auth.reset.ForgotPasswordRequest;
import com.medinote.medinotebackend.auth.reset.ResetPasswordRequest;
import com.medinote.medinotebackend.auth.reset.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserRepository userRepo,PasswordResetService passwordResetService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        User user = userRepo.findByEmail(req.email()).orElseThrow();
        String token = jwtService.generateToken(user.getEmail());

        return ResponseEntity.ok(new LoginResponse(token, user.getRole().name(), user.getEmail()));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logged out successfully");
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

}
