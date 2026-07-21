package com.minimarket.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Datos públicos para registrar una cuenta cliente")
public class RegisterRequest {

    @Schema(example = "nuevoCliente", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(example = "nuevo.cliente@minimarket.local", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correo;

    @Schema(example = "cliente123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Roles solicitados. El registro público solo acepta ROLE_CLIENTE.")
    private List<String> roles;

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
