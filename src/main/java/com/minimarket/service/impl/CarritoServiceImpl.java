package com.minimarket.service.impl;

import com.minimarket.entity.Carrito;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.CarritoRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.service.CarritoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CarritoServiceImpl implements CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    public CarritoServiceImpl(
            CarritoRepository carritoRepository,
            UsuarioRepository usuarioRepository,
            ProductoRepository productoRepository) {
        this.carritoRepository = carritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public List<Carrito> findAll() {
        Authentication authentication = authenticationActual();
        if (tieneRol(authentication, "ROLE_ADMIN")) {
            return carritoRepository.findAll();
        }
        Usuario usuario = usuarioAutenticado(authentication);
        if (tieneRol(authentication, "ROLE_CLIENTE")) {
            return carritoRepository.findByUsuarioId(usuario.getId());
        }
        throw new AccessDeniedException("No autorizado para consultar carritos");
    }

    @Override
    public Carrito findById(Long id) {
        Carrito carrito = carritoRepository.findById(id).orElse(null);
        if (carrito == null) {
            return null;
        }
        validarAcceso(carrito);
        return carrito;
    }

    @Override
    public Carrito save(Carrito carrito) {
        if (carrito == null) {
            throw new IllegalArgumentException("Debe ingresar datos del carrito");
        }

        Authentication authentication = authenticationActual();
        Carrito existente = carrito.getId() == null
                ? null
                : carritoRepository.findById(carrito.getId()).orElse(null);
        if (carrito.getId() != null && existente == null) {
            throw new IllegalArgumentException("Registro de carrito no encontrado");
        }
        if (existente != null) {
            validarAcceso(existente, authentication);
        }

        carrito.setUsuario(resolverUsuario(carrito, existente, authentication));
        carrito.setProducto(resolverProducto(carrito, existente));
        carrito.setCantidad(resolverCantidad(carrito, existente));
        return carritoRepository.save(carrito);
    }

    @Override
    public void deleteById(Long id) {
        Carrito carrito = carritoRepository.findById(id).orElse(null);
        if (carrito != null) {
            validarAcceso(carrito);
            carritoRepository.deleteById(id);
        }
    }

    @Override
    public List<Carrito> findByUsuarioId(Long usuarioId) {
        Authentication authentication = authenticationActual();
        if (tieneRol(authentication, "ROLE_ADMIN")) {
            return carritoRepository.findByUsuarioId(usuarioId);
        }
        Usuario usuario = usuarioAutenticado(authentication);
        if (tieneRol(authentication, "ROLE_CLIENTE") && Objects.equals(usuario.getId(), usuarioId)) {
            return carritoRepository.findByUsuarioId(usuarioId);
        }
        throw new AccessDeniedException("No autorizado para consultar carritos de otro usuario");
    }

    private Authentication authenticationActual() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> rol.equals(authority.getAuthority()));
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado"));
    }

    private Usuario resolverUsuario(Carrito carrito, Carrito existente, Authentication authentication) {
        if (tieneRol(authentication, "ROLE_ADMIN")) {
            if (carrito.getUsuario() != null && carrito.getUsuario().getId() != null) {
                return usuarioRepository.findById(carrito.getUsuario().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            }
            if (existente != null && existente.getUsuario() != null) {
                return existente.getUsuario();
            }
            throw new IllegalArgumentException("Debe indicar usuario del carrito");
        }

        if (tieneRol(authentication, "ROLE_CLIENTE")) {
            Usuario usuario = usuarioAutenticado(authentication);
            if (existente != null && !perteneceAlUsuario(existente, usuario)) {
                throw new AccessDeniedException("El carrito pertenece a otro usuario");
            }
            return usuario;
        }

        throw new AccessDeniedException("No autorizado para administrar carritos");
    }

    private Producto resolverProducto(Carrito carrito, Carrito existente) {
        if (carrito.getProducto() != null && carrito.getProducto().getId() != null) {
            return productoRepository.findById(carrito.getProducto().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        }
        if (existente != null && existente.getProducto() != null) {
            return existente.getProducto();
        }
        throw new IllegalArgumentException("Debe indicar producto del carrito");
    }

    private Integer resolverCantidad(Carrito carrito, Carrito existente) {
        Integer cantidad = carrito.getCantidad();
        if (cantidad == null && existente != null) {
            cantidad = existente.getCantidad();
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        return cantidad;
    }

    private void validarAcceso(Carrito carrito) {
        validarAcceso(carrito, authenticationActual());
    }

    private void validarAcceso(Carrito carrito, Authentication authentication) {
        if (tieneRol(authentication, "ROLE_ADMIN")) {
            return;
        }
        Usuario usuario = usuarioAutenticado(authentication);
        if (tieneRol(authentication, "ROLE_CLIENTE") && perteneceAlUsuario(carrito, usuario)) {
            return;
        }
        throw new AccessDeniedException("El carrito pertenece a otro usuario");
    }

    private boolean perteneceAlUsuario(Carrito carrito, Usuario usuario) {
        return carrito.getUsuario() != null
                && carrito.getUsuario().getId() != null
                && usuario.getId() != null
                && Objects.equals(carrito.getUsuario().getId(), usuario.getId());
    }
}
