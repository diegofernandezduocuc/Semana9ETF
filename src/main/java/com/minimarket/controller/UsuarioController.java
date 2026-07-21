package com.minimarket.controller;

import com.minimarket.config.ApiExamples;
import com.minimarket.entity.Usuario;
import com.minimarket.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/usuarios", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Usuarios", description = "Administración de clientes y roles del sistema.")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Obtiene los usuarios registrados con enlaces HATEOAS.")
    @ApiResponse(responseCode = "200", description = "Usuarios obtenidos correctamente",
            content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                    examples = @ExampleObject(value = ApiExamples.USUARIOS_COLLECTION)))
    public CollectionModel<EntityModel<Usuario>> listarUsuarios() {
        List<EntityModel<Usuario>> usuarios = usuarioService.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(usuarios,
                linkTo(methodOn(UsuarioController.class).listarUsuarios()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario", description = "Busca un usuario por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Usuario.class),
                            examples = @ExampleObject(value = ApiExamples.USUARIO_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    public ResponseEntity<EntityModel<Usuario>> obtenerUsuarioPorId(
            @Parameter(description = "Identificador del usuario", example = "2") @PathVariable Long id) {
        Optional<Usuario> usuario = usuarioService.findById(id);
        return usuario
                .map(value -> ResponseEntity.ok(toModel(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Crear usuario", description = "Registra un usuario y cifra su contraseña.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            examples = @ExampleObject(value = ApiExamples.USUARIO_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Datos o roles inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "El nombre de usuario ya existe", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol administrador", content = @Content)
    })
    public ResponseEntity<EntityModel<Usuario>> guardarUsuario(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos y roles del usuario",
                    content = @Content(schema = @Schema(implementation = Usuario.class),
                            examples = @ExampleObject(value = ApiExamples.USUARIO_REQUEST)))
            @RequestBody Usuario usuario) {
        Usuario guardado = usuarioService.save(usuario);
        return ResponseEntity
                .created(linkTo(methodOn(UsuarioController.class).obtenerUsuarioPorId(guardado.getId())).toUri())
                .body(toModel(guardado));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Actualizar usuario", description = "Modifica los datos y roles de un usuario existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol administrador", content = @Content)
    })
    public ResponseEntity<EntityModel<Usuario>> actualizarUsuario(
            @Parameter(description = "Identificador del usuario", example = "2") @PathVariable Long id,
            @RequestBody Usuario usuario) {
        Optional<Usuario> existente = usuarioService.findById(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        usuario.setId(id);
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            usuario.setPassword(existente.get().getPassword());
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            usuario.setCorreo(existente.get().getCorreo());
        }
        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            usuario.setRoles(existente.get().getRoles());
        }
        return ResponseEntity.ok(toModel(usuarioService.save(usuario)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario registrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol administrador", content = @Content)
    })
    public ResponseEntity<Void> eliminarUsuario(
            @Parameter(description = "Identificador del usuario", example = "2") @PathVariable Long id) {
        if (usuarioService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        usuarioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<Usuario> toModel(Usuario usuario) {
        return EntityModel.of(usuario,
                linkTo(methodOn(UsuarioController.class).obtenerUsuarioPorId(usuario.getId())).withSelfRel(),
                linkTo(methodOn(UsuarioController.class).listarUsuarios()).withRel("usuarios"),
                linkTo(methodOn(CarritoController.class).listarCarrito()).withRel("carrito"),
                linkTo(methodOn(VentaController.class).listarVentas()).withRel("ventas"));
    }
}
