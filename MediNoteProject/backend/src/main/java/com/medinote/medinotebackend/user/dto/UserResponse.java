package com.medinote.medinotebackend.user.dto;

import com.medinote.medinotebackend.user.Role;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean enabled
) {}
