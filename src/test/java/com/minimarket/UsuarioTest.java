package com.minimarket;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {

    @Test
    void creaUsuarioConRol() {
        Usuario usuario = new Usuario();
        usuario.setUsername("adminUser");
        usuario.setCorreo("admin.user@minimarket.local");
        usuario.setPassword("securePassword123");
        usuario.setRoles(Set.of(new Rol("ROLE_ADMIN")));

        assertNotNull(usuario);
        assertEquals("adminUser", usuario.getUsername());
        assertEquals("admin.user@minimarket.local", usuario.getCorreo());
        assertEquals(1, usuario.getRoles().size());
        assertTrue(usuario.getRoles().stream()
                .anyMatch(rol -> rol.getNombre().equals("ROLE_ADMIN")));
    }

    @Test
    void conservaDatosAsignados() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("clienteUser");
        usuario.setCorreo("cliente.user@minimarket.local");
        usuario.setPassword("claveSegura");

        assertEquals(1L, usuario.getId());
        assertEquals("clienteUser", usuario.getUsername());
        assertEquals("cliente.user@minimarket.local", usuario.getCorreo());
        assertEquals("claveSegura", usuario.getPassword());
    }

    @Test
    void permiteAsignarVariosRoles() {
        Usuario usuario = new Usuario();
        usuario.setRoles(Set.of(new Rol("ROLE_CLIENTE"), new Rol("ROLE_ADMIN")));

        assertEquals(2, usuario.getRoles().size());
    }
}
