package com.deliverytest.api.auth.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) { }
