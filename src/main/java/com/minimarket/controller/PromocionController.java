package com.minimarket.controller;

import com.minimarket.dto.PromocionRequest;
import com.minimarket.entity.Promocion;
import com.minimarket.service.PromocionService;
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
@RequestMapping(value = "/api/promociones", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Promociones", description = "Gestión centralizada de ofertas y promociones.")
public class PromocionController {

    private final PromocionService service;

    public PromocionController(PromocionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar promociones", description = "Obtiene todas las promociones. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promociones obtenidas correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content)
    })
    public CollectionModel<EntityModel<Promocion>> listar() {
        return collection(service.findAll(), false);
    }

    @GetMapping("/activas")
    @Operation(
            summary = "Listar promociones activas",
            description = "Obtiene las promociones vigentes para los usuarios autenticados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promociones activas obtenidas correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<Promocion>> activas() {
        return collection(service.findActivas(), true);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener promoción", description = "Obtiene una promoción por su identificador. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promoción encontrada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Promocion.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Promoción no encontrada", content = @Content)
    })
    public EntityModel<Promocion> obtener(
            @Parameter(description = "Identificador de la promoción", example = "1")
            @PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Crear promoción", description = "Registra una promoción para un producto. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promoción creada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Promocion.class))),
            @ApiResponse(responseCode = "400", description = "Datos o fechas inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content)
    })
    public ResponseEntity<EntityModel<Promocion>> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos de la promoción",
                    content = @Content(schema = @Schema(implementation = PromocionRequest.class)))
            @RequestBody PromocionRequest request) {
        Promocion guardada = service.save(request, null);
        return ResponseEntity
                .created(linkTo(methodOn(PromocionController.class).obtener(guardada.getId())).toUri())
                .body(toModel(guardada));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Actualizar promoción", description = "Actualiza una promoción existente. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promoción actualizada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Promocion.class))),
            @ApiResponse(responseCode = "400", description = "Datos o fechas inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Promoción o producto no encontrado", content = @Content)
    })
    public EntityModel<Promocion> actualizar(
            @Parameter(description = "Identificador de la promoción", example = "1")
            @PathVariable Long id,
            @RequestBody PromocionRequest request) {
        return toModel(service.save(request, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar promoción", description = "Elimina una promoción existente. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Promoción eliminada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Promoción no encontrada", content = @Content)
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador de la promoción", example = "1")
            @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CollectionModel<EntityModel<Promocion>> collection(List<Promocion> items, boolean soloActivas) {
        CollectionModel<EntityModel<Promocion>> model = CollectionModel.of(
                items.stream().map(this::toModel).toList());
        if (soloActivas) {
            model.add(linkTo(methodOn(PromocionController.class).activas()).withSelfRel());
            model.add(linkTo(methodOn(PromocionController.class).listar()).withRel("promociones"));
        } else {
            model.add(linkTo(methodOn(PromocionController.class).listar()).withSelfRel());
            model.add(linkTo(methodOn(PromocionController.class).activas()).withRel("activas"));
        }
        return model;
    }

    private EntityModel<Promocion> toModel(Promocion promocion) {
        return EntityModel.of(promocion,
                linkTo(methodOn(PromocionController.class).obtener(promocion.getId())).withSelfRel(),
                linkTo(methodOn(PromocionController.class).activas()).withRel("promociones-activas"),
                linkTo(methodOn(ProductoController.class)
                        .obtenerProductoPorId(promocion.getProducto().getId())).withRel("producto"));
    }
}
