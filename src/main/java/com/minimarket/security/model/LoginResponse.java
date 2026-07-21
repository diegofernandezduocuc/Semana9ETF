package com.minimarket.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Token JWT generado después de autenticar al usuario")
public record LoginResponse(
        @Schema(description = "Token firmado") String token,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "3600") long expiresIn,
        @Schema(example = "admin") String username,
        List<String> roles) {
}
