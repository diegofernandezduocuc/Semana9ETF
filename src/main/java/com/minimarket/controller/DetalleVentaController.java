package com.minimarket.controller;

import com.minimarket.config.ApiExamples;
import com.minimarket.entity.DetalleVenta;
import com.minimarket.service.DetalleVentaService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/detalle-ventas", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Detalle de ventas", description = "Consulta de productos asociados a ventas.")
public class DetalleVentaController {

    private final DetalleVentaService detalleVentaService;

    public DetalleVentaController(DetalleVentaService detalleVentaService) {
        this.detalleVentaService = detalleVentaService;
    }

    @GetMapping
    @Operation(summary = "Listar detalles de venta", description = "Obtiene los detalles de venta con enlaces HATEOAS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalles obtenidos correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<DetalleVenta>> listarDetalleVentas() {
        List<EntityModel<DetalleVenta>> detalles = detalleVentaService.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(detalles,
                linkTo(methodOn(DetalleVentaController.class).listarDetalleVentas()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de venta", description = "Busca un detalle de venta por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle encontrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = DetalleVenta.class),
                            examples = @ExampleObject(value = ApiExamples.DETALLE_VENTA_RESPONSE))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Detalle no encontrado", content = @Content)
    })
    public ResponseEntity<EntityModel<DetalleVenta>> obtenerDetalleVentaPorId(@PathVariable Long id) {
        DetalleVenta detalle = detalleVentaService.findById(id);
        return detalle == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(toModel(detalle));
    }

    EntityModel<DetalleVenta> toModel(DetalleVenta detalle) {
        EntityModel<DetalleVenta> model = EntityModel.of(detalle,
                linkTo(methodOn(DetalleVentaController.class).obtenerDetalleVentaPorId(detalle.getId())).withSelfRel(),
                linkTo(methodOn(DetalleVentaController.class).listarDetalleVentas()).withRel("detalle-ventas"),
                linkTo(methodOn(VentaController.class).listarVentas()).withRel("ventas"),
                linkTo(methodOn(InventarioController.class).listarMovimientosDeInventario()).withRel("inventario"));

        if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
            model.add(linkTo(methodOn(ProductoController.class)
                    .obtenerProductoPorId(detalle.getProducto().getId())).withRel("producto"));
        }
        if (detalle.getVenta() != null && detalle.getVenta().getId() != null) {
            model.add(linkTo(methodOn(VentaController.class)
                    .obtenerVentaPorId(detalle.getVenta().getId())).withRel("venta"));
            model.add(linkTo(methodOn(VentaController.class)
                    .listarDetallesDeVenta(detalle.getVenta().getId())).withRel("detalles-venta"));
        }
        return model;
    }
}
