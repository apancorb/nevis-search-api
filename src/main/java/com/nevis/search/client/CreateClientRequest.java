package com.nevis.search.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateClientRequest(
        @NotBlank(message = "first_name is required")
        String firstName,

        @NotBlank(message = "last_name is required")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        String description,

        List<String> socialLinks
) {}
