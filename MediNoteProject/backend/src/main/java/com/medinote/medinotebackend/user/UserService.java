package com.medinote.medinotebackend.user;

import com.medinote.medinotebackend.user.dto.UserRequest;
import com.medinote.medinotebackend.user.dto.UserResponse;
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
        String me = SecurityContextHolder.getContext().getAuthentication().getName(); // email
        return repo.findAll().stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(me))
                .map(this::toResponse)
                .toList();
    }
    public UserResponse findById(Long id) {
        User u = repo.findById(id).orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        return toResponse(u);
    }
    public UserResponse create(UserRequest req) {
        if (repo.existsByEmail(req.email())) throw new RuntimeException("EMAIL_ALREADY_USED");

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
        User u = repo.findById(id).orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        if (req.email() != null && !req.email().equals(u.getEmail())) {
            if (repo.existsByEmail(req.email())) throw new RuntimeException("EMAIL_ALREADY_USED");
            u.setEmail(req.email());
        }
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.role() != null) u.setRole(req.role());
        if (req.enabled() != null) u.setEnabled(req.enabled());

        // update password seulement si fourni
        if (req.password() != null && !req.password().isBlank()) {
            u.setPassword(encoder.encode(req.password()));
        }

        return toResponse(repo.save(u));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new RuntimeException("USER_NOT_FOUND");
        repo.deleteById(id);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled());
    }
}
