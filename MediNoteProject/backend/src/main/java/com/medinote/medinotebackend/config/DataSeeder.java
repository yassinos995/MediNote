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
    CommandLineRunner seedUsers(UserRepository repo, PasswordEncoder encoder) {
        return args -> {

            // ---------- ADMIN ----------
            if (!repo.existsByEmail("admin@medinote.com")) {
                repo.save(User.builder()
                        .email("admin@medinote.com")
                        .fullName("System Admin")
                        .password(encoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build());

                System.out.println("✅ Admin created");
            }

            // ---------- DELEGATE ----------
            if (!repo.existsByEmail("delegate@medinote.com")) {
                repo.save(User.builder()
                        .email("delegate@medinote.com")
                        .fullName("Medical Delegate")
                        .password(encoder.encode("delegate123"))
                        .role(Role.DELEGATE)
                        .enabled(true)
                        .build());

                System.out.println("✅ Delegate created");
            }

            // ---------- STAFF ----------
            if (!repo.existsByEmail("staff@medinote.com")) {
                repo.save(User.builder()
                        .email("staff@medinote.com")
                        .fullName("Staff User")
                        .password(encoder.encode("staff123"))
                        .role(Role.STAFF)
                        .enabled(true)
                        .build());

                System.out.println("✅ Staff created");
            }
        };
    }
}
