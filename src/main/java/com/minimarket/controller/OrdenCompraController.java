package com.minimarket.controller;

import com.minimarket.entity.OrdenCompra;
import com.minimarket.service.OrdenCompraService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/ordenes-compra", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Órdenes de compra", description = "Órdenes automáticas de reposición a proveedores.")
public class OrdenCompraController {

    private final OrdenCompraService service;

    public OrdenCompraController(OrdenCompraService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar órdenes de compra", description = "Obtiene todas las órdenes de compra. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Órdenes obtenidas correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content)
    })
    public CollectionModel<EntityModel<OrdenCompra>> listar() {
        return collection(service.findAll(), false);
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Listar órdenes pendientes", description = "Obtiene las órdenes que aún requieren recepción o cancelación. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Órdenes pendientes obtenidas correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content)
    })
    public CollectionModel<EntityModel<OrdenCompra>> pendientes() {
        return collection(service.findPendientes(), true);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener orden de compra", description = "Obtiene una orden por su identificador. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden encontrada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = OrdenCompra.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content)
    })
    public EntityModel<OrdenCompra> obtener(
            @Parameter(description = "Identificador de la orden", example = "1")
            @PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @PostMapping("/{id}/recibir")
    @Operation(
            summary = "Recibir orden de compra",
            description = "Marca la orden como RECIBIDA y repone el stock de la sucursal dentro de una transacción."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden recibida y stock repuesto",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = OrdenCompra.class))),
            @ApiResponse(responseCode = "400", description = "La orden no está pendiente", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Orden o stock asociado no encontrado", content = @Content)
    })
    public EntityModel<OrdenCompra> recibir(
            @Parameter(description = "Identificador de la orden", example = "1")
            @PathVariable Long id) {
        return toModel(service.recibir(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar orden de compra", description = "Marca una orden pendiente como CANCELADA. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden cancelada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = OrdenCompra.class))),
            @ApiResponse(responseCode = "400", description = "La orden no está pendiente", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content)
    })
    public EntityModel<OrdenCompra> cancelar(
            @Parameter(description = "Identificador de la orden", example = "1")
            @PathVariable Long id) {
        return toModel(service.cancelar(id));
    }

    private CollectionModel<EntityModel<OrdenCompra>> collection(List<OrdenCompra> items, boolean soloPendientes) {
        CollectionModel<EntityModel<OrdenCompra>> model = CollectionModel.of(
                items.stream().map(this::toModel).toList(),
                linkTo(methodOn(OrdenCompraController.class).listar()).withRel("ordenes-compra"));
        if (soloPendientes) {
            model.add(linkTo(methodOn(OrdenCompraController.class).pendientes()).withSelfRel());
        } else {
            model.add(linkTo(methodOn(OrdenCompraController.class).listar()).withSelfRel());
            model.add(linkTo(methodOn(OrdenCompraController.class).pendientes()).withRel("pendientes"));
        }
        return model;
    }

    private EntityModel<OrdenCompra> toModel(OrdenCompra orden) {
        EntityModel<OrdenCompra> model = EntityModel.of(orden,
                linkTo(methodOn(OrdenCompraController.class).obtener(orden.getId())).withSelfRel(),
                linkTo(methodOn(OrdenCompraController.class).listar()).withRel("ordenes-compra"),
                linkTo(methodOn(SucursalController.class).obtener(orden.getSucursal().getId())).withRel("sucursal"),
                linkTo(methodOn(ProveedorController.class).obtener(orden.getProveedor().getId())).withRel("proveedor"),
                linkTo(methodOn(ProductoController.class)
                        .obtenerProductoPorId(orden.getProducto().getId())).withRel("producto"));
        if ("PENDIENTE".equals(orden.getEstado())) {
            model.add(linkTo(methodOn(OrdenCompraController.class).recibir(orden.getId())).withRel("recibir"));
            model.add(linkTo(methodOn(OrdenCompraController.class).cancelar(orden.getId())).withRel("cancelar"));
        }
        return model;
    }
}
