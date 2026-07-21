package com.minimarket.service.impl;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.service.UsuarioService;
import com.minimarket.service.UsuarioValidation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Override
    public Usuario save(Usuario usuario) {
        validarDatosBasicos(usuario);
        if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        usuario.setRoles(resolverRoles(usuario.getRoles()));
        return usuarioRepository.save(usuario);
    }

    private void validarDatosBasicos(Usuario usuario) {
        usuario.setUsername(UsuarioValidation.validarUsername(usuario.getUsername()));
        usuario.setCorreo(UsuarioValidation.validarCorreo(usuario.getCorreo()));
        UsuarioValidation.validarPassword(usuario.getPassword());
    }

    private Set<Rol> resolverRoles(Set<Rol> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of(rolRepository.findByNombre("ROLE_CLIENTE")
                    .orElseThrow(() -> new IllegalStateException("Rol de cliente no configurado")));
        }

        Set<Rol> rolesPersistidos = new HashSet<>();
        for (Rol rol : roles) {
            if (rol.getId() != null) {
                rolesPersistidos.add(rolRepository.findById(rol.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado")));
            } else if (rol.getNombre() != null) {
                rolesPersistidos.add(rolRepository.findByNombre(rol.getNombre())
                        .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado")));
            }
        }
        return rolesPersistidos;
    }

    @Override
    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }
}
