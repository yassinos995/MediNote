package com.medinote.medinotebackend.config;

import com.medinote.medinotebackend.user.Role;
import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            String email = "admin@medinote.com";

            if (!repo.existsByEmail(email)) {
                repo.save(User.builder()
                        .email(email)
                        .fullName("System Admin")
                        .password(encoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build());

                System.out.println("✅ Admin created: admin@medinote.com / admin123");
            }
        };
    }
}
