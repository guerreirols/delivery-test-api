package com.deliverytest.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "{validation.auth.name.required}")
        @Size(max = 120, message = "{validation.auth.name.size}") String name,
        @NotBlank(message = "{validation.auth.email.required}")
        @Email(message = "{validation.auth.email.invalid}")
        @Size(max = 254, message = "{validation.auth.email.size}") String email,
        @NotBlank(message = "{validation.auth.password.required}")
        @Size(min = 8, max = 64, message = "{validation.auth.password.size}") String password
) { }
