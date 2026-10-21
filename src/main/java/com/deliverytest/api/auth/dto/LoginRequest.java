package com.deliverytest.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "{validation.auth.email.required}")
        @Email(message = "{validation.auth.email.invalid}")
        @Size(max = 254, message = "{validation.auth.email.size}") String email,
        @NotBlank(message = "{validation.auth.password.required}")
        @Size(max = 64, message = "{validation.auth.password.size}") String password
) { }
