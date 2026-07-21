package com.minimarket.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cuenta cliente registrada")
public record RegisterResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "nuevoCliente") String username,
        @Schema(example = "nuevo.cliente@minimarket.local") String correo,
        List<String> roles) {
}
