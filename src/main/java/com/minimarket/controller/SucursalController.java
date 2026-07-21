package com.minimarket.controller;

import com.minimarket.entity.Sucursal;
import com.minimarket.service.SucursalService;
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
@RequestMapping(value = "/api/sucursales", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Sucursales", description = "Gestión y consulta de sucursales.")
public class SucursalController {

    private final SucursalService service;

    public SucursalController(SucursalService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Listar sucursales",
            description = "Obtiene las sucursales registradas y enlaces HATEOAS hacia su stock."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursales obtenidas correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<Sucursal>> listar() {
        List<EntityModel<Sucursal>> items = service.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(items,
                linkTo(methodOn(SucursalController.class).listar()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener sucursal",
            description = "Obtiene una sucursal por su identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal encontrada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Sucursal.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada", content = @Content)
    })
    public EntityModel<Sucursal> obtener(
            @Parameter(description = "Identificador de la sucursal", example = "1")
            @PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @PostMapping(consumes = "application/json")
    @Operation(
            summary = "Crear sucursal",
            description = "Crea una sucursal. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sucursal creada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Sucursal.class))),
            @ApiResponse(responseCode = "400", description = "Nombre o dirección inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "409", description = "Nombre de sucursal duplicado", content = @Content)
    })
    public ResponseEntity<EntityModel<Sucursal>> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos de la sucursal",
                    content = @Content(schema = @Schema(implementation = Sucursal.class)))
            @RequestBody Sucursal sucursal) {
        Sucursal guardada = service.save(sucursal);
        return ResponseEntity
                .created(linkTo(methodOn(SucursalController.class).obtener(guardada.getId())).toUri())
                .body(toModel(guardada));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @Operation(
            summary = "Actualizar sucursal",
            description = "Actualiza los datos de una sucursal existente. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal actualizada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Sucursal.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Nombre de sucursal duplicado", content = @Content)
    })
    public EntityModel<Sucursal> actualizar(
            @Parameter(description = "Identificador de la sucursal", example = "1")
            @PathVariable Long id,
            @RequestBody Sucursal sucursal) {
        return toModel(service.update(id, sucursal));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar sucursal",
            description = "Elimina una sucursal existente. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sucursal eliminada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La sucursal posee relaciones activas", content = @Content)
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador de la sucursal", example = "1")
            @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<Sucursal> toModel(Sucursal sucursal) {
        return EntityModel.of(sucursal,
                linkTo(methodOn(SucursalController.class).obtener(sucursal.getId())).withSelfRel(),
                linkTo(methodOn(SucursalController.class).listar()).withRel("sucursales"),
                linkTo(methodOn(StockSucursalController.class)
                        .listarPorSucursal(sucursal.getId())).withRel("stock"));
    }
}
