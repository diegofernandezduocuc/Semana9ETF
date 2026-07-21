package com.minimarket.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credenciales para obtener un token JWT")
public class LoginRequest {

    @Schema(example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
