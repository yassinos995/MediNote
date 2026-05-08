package com.medinote.medinotebackend.user;

import com.medinote.medinotebackend.user.dto.UserRequest;
import com.medinote.medinotebackend.user.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public List<UserResponse> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String me = (auth != null && auth.isAuthenticated()) ? auth.getName() : "";
        return repo.findAll().stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(me))
                .map(this::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        User u = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return toResponse(u);
    }

    public UserResponse create(UserRequest req) {
        if (req.email() == null || req.email().isBlank())
            throw new IllegalArgumentException("email is required");
        if (req.password() == null || req.password().isBlank())
            throw new IllegalArgumentException("password is required");
        if (repo.existsByEmail(req.email()))
            throw new IllegalStateException("Email already in use: " + req.email());

        User u = User.builder()
                .email(req.email())
                .fullName(req.fullName())
                .password(encoder.encode(req.password()))
                .role(req.role() == null ? Role.STAFF : req.role())
                .enabled(req.enabled() == null || req.enabled())
                .build();

        return toResponse(repo.save(u));
    }

    public UserResponse update(Long id, UserRequest req) {
        User u = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        if (req.email() != null && !req.email().equals(u.getEmail())) {
            if (repo.existsByEmail(req.email()))
                throw new IllegalStateException("Email already in use: " + req.email());
            u.setEmail(req.email());
        }
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.role() != null) u.setRole(req.role());
        if (req.enabled() != null) u.setEnabled(req.enabled());

        if (req.password() != null && !req.password().isBlank()) {
            u.setPassword(encoder.encode(req.password()));
        }

        return toResponse(repo.save(u));
    }

    public void delete(Long id) {
        if (!repo.existsById(id))
            throw new EntityNotFoundException("User not found: " + id);
        repo.deleteById(id);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled());
    }
}
