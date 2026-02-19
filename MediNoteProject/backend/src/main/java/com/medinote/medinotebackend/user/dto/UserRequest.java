package com.medinote.medinotebackend.user.dto;

import com.medinote.medinotebackend.user.Role;

public record UserRequest(
        String email,
        String fullName,
        String password, // pour create (et update si tu veux)
        Role role,
        Boolean enabled
) {}
