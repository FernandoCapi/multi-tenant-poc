package com.vesta.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
