package com.minimarket.service;

import java.util.Locale;
import java.util.regex.Pattern;

public final class UsuarioValidation {

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int CORREO_MAX_LENGTH = 120;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final Pattern CORREO_BASICO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private UsuarioValidation() {
    }

    public static String validarUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar nombre de usuario");
        }
        String normalizado = username.trim();
        if (normalizado.length() < USERNAME_MIN_LENGTH || normalizado.length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("El usuario debe tener entre 3 y 50 caracteres");
        }
        return normalizado;
    }

    public static String validarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar correo");
        }
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > CORREO_MAX_LENGTH) {
            throw new IllegalArgumentException("El correo no puede superar 120 caracteres");
        }
        if (!CORREO_BASICO.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("El correo debe tener formato válido");
        }
        return normalizado;
    }

    public static void validarPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar contraseña");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
    }
}
