package com.minimarket.controller;

import com.minimarket.config.ApiExamples;
import com.minimarket.dto.InventarioRequest;
import com.minimarket.entity.Inventario;
import com.minimarket.service.InventarioService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/inventario", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Inventario", description = "Registro y consulta de movimientos de stock.")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    @Operation(summary = "Listar movimientos", description = "Obtiene el historial de movimientos de inventario con enlaces HATEOAS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos obtenidos correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            examples = @ExampleObject(value = ApiExamples.INVENTARIO_COLLECTION))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<Inventario>> listarMovimientosDeInventario() {
        List<EntityModel<Inventario>> movimientos = inventarioService.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(movimientos,
                linkTo(methodOn(InventarioController.class).listarMovimientosDeInventario()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener movimiento", description = "Busca un movimiento por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimiento encontrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Inventario.class),
                            examples = @ExampleObject(value = ApiExamples.INVENTARIO_RESPONSE))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado", content = @Content)
    })
    public ResponseEntity<EntityModel<Inventario>> obtenerMovimientoPorId(
            @Parameter(description = "Identificador del movimiento", example = "1") @PathVariable Long id) {
        Inventario inventario = inventarioService.findById(id);
        return inventario == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(toModel(inventario));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Registrar movimiento",
            description = "Crea un nuevo movimiento inmutable, actualiza el stock del producto y evita stock negativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimiento registrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            examples = @ExampleObject(value = ApiExamples.INVENTARIO_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol administrador", content = @Content),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content)
    })
    public ResponseEntity<EntityModel<Inventario>> registrarMovimiento(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identificador de producto, cantidad y tipo de movimiento",
                    content = @Content(schema = @Schema(implementation = InventarioRequest.class),
                            examples = @ExampleObject(value = ApiExamples.INVENTARIO_REQUEST)))
            @RequestBody InventarioRequest inventario) {
        Inventario guardado = inventarioService.registrarMovimiento(inventario);
        return ResponseEntity
                .created(linkTo(methodOn(InventarioController.class).obtenerMovimientoPorId(guardado.getId())).toUri())
                .body(toModel(guardado));
    }

    private EntityModel<Inventario> toModel(Inventario inventario) {
        EntityModel<Inventario> model = EntityModel.of(inventario,
                linkTo(methodOn(InventarioController.class).obtenerMovimientoPorId(inventario.getId())).withSelfRel(),
                linkTo(methodOn(InventarioController.class).listarMovimientosDeInventario()).withRel("inventario"),
                linkTo(methodOn(ProductoController.class).listarProductos()).withRel("productos"));

        if (inventario.getProducto() != null && inventario.getProducto().getId() != null) {
            model.add(linkTo(methodOn(ProductoController.class)
                    .obtenerProductoPorId(inventario.getProducto().getId())).withRel("producto"));
        }
        return model;
    }
}
