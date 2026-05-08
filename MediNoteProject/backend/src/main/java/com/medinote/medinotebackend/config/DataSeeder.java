package com.medinote.medinotebackend.config;

import com.medinote.medinotebackend.user.Role;
import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(
            UserRepository repo,
            PasswordEncoder encoder,
            @Value("${app.seed.enabled:false}") boolean seedEnabled,
            @Value("${app.seed.admin.email:}") String adminEmail,
            @Value("${app.seed.admin.password:}") String adminPassword,
            @Value("${app.seed.staff.email:}") String staffEmail,
            @Value("${app.seed.staff.password:}") String staffPassword
    ) {
        return args -> {
            if (!seedEnabled) {
                return;
            }

            seedUser(repo, encoder, adminEmail, adminPassword, "System Admin", Role.ADMIN);
            seedUser(repo, encoder, staffEmail, staffPassword, "Staff User", Role.STAFF);
        };
    }

    private void seedUser(
            UserRepository repo,
            PasswordEncoder encoder,
            String email,
            String password,
            String fullName,
            Role role
    ) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim();
        if (!repo.existsByEmail(normalizedEmail)) {
            repo.save(User.builder()
                    .email(normalizedEmail)
                    .fullName(fullName)
                    .password(encoder.encode(password))
                    .role(role)
                    .enabled(true)
                    .build());
            System.out.println("Seeded " + role + " user: " + normalizedEmail);
        }
    }
}
