package com.minimarket.controller;

import com.minimarket.entity.Proveedor;
import com.minimarket.service.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/proveedores", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Proveedores", description = "Administración de proveedores.")
public class ProveedorController {

    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar proveedores", description = "Obtiene todos los proveedores. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedores obtenidos correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content)
    })
    public CollectionModel<EntityModel<Proveedor>> listar() {
        List<EntityModel<Proveedor>> items = service.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(items,
                linkTo(methodOn(ProveedorController.class).listar()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener proveedor", description = "Obtiene un proveedor por su identificador. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor encontrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Proveedor.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content)
    })
    public EntityModel<Proveedor> obtener(
            @Parameter(description = "Identificador del proveedor", example = "1")
            @PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Crear proveedor", description = "Registra un proveedor. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor creado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Proveedor.class))),
            @ApiResponse(responseCode = "400", description = "Nombre o correo inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "409", description = "Correo de proveedor duplicado", content = @Content)
    })
    public ResponseEntity<EntityModel<Proveedor>> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos del proveedor",
                    content = @Content(schema = @Schema(implementation = Proveedor.class)))
            @RequestBody Proveedor proveedor) {
        Proveedor guardado = service.save(proveedor);
        return ResponseEntity
                .created(linkTo(methodOn(ProveedorController.class).obtener(guardado.getId())).toUri())
                .body(toModel(guardado));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Actualizar proveedor", description = "Actualiza un proveedor existente. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor actualizado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Proveedor.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Correo de proveedor duplicado", content = @Content)
    })
    public EntityModel<Proveedor> actualizar(
            @Parameter(description = "Identificador del proveedor", example = "1")
            @PathVariable Long id,
            @RequestBody Proveedor proveedor) {
        return toModel(service.update(id, proveedor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar proveedor", description = "Elimina un proveedor existente. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proveedor eliminado"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "El proveedor posee órdenes relacionadas", content = @Content)
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del proveedor", example = "1")
            @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<Proveedor> toModel(Proveedor proveedor) {
        return EntityModel.of(proveedor,
                linkTo(methodOn(ProveedorController.class).obtener(proveedor.getId())).withSelfRel(),
                linkTo(methodOn(ProveedorController.class).listar()).withRel("proveedores"),
                linkTo(methodOn(OrdenCompraController.class).listar()).withRel("ordenes-compra"));
    }
}
