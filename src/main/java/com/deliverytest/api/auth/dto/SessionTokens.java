package com.deliverytest.api.auth.dto;

public record SessionTokens(AuthResponse access, String refreshToken) { }
