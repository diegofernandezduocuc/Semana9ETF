package com.minimarket.controller;

import com.minimarket.config.ApiExamples;
import com.minimarket.dto.VentaRequest;
import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Venta;
import com.minimarket.service.DetalleVentaService;
import com.minimarket.service.VentaService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/ventas", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Ventas", description = "Consulta y registro de ventas.")
public class VentaController {

    private final VentaService ventaService;
    private final DetalleVentaService detalleVentaService;

    public VentaController(VentaService ventaService, DetalleVentaService detalleVentaService) {
        this.ventaService = ventaService;
        this.detalleVentaService = detalleVentaService;
    }

    @GetMapping
    @Operation(summary = "Listar ventas", description = "Obtiene las ventas registradas con enlaces HATEOAS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ventas obtenidas correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<Venta>> listarVentas() {
        List<EntityModel<Venta>> ventas = ventaService.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(ventas,
                linkTo(methodOn(VentaController.class).listarVentas()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta", description = "Busca una venta por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta encontrada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Venta.class),
                            examples = @ExampleObject(value = ApiExamples.VENTA_RESPONSE))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada", content = @Content)
    })
    public ResponseEntity<EntityModel<Venta>> obtenerVentaPorId(@PathVariable Long id) {
        Venta venta = ventaService.findById(id);
        return venta == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(toModel(venta));
    }

    @GetMapping("/{ventaId}/detalles")
    @Operation(summary = "Listar detalles de una venta",
            description = "Obtiene solo los detalles asociados a la venta indicada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalles de venta obtenidos",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada", content = @Content)
    })
    public ResponseEntity<CollectionModel<EntityModel<DetalleVenta>>> listarDetallesDeVenta(@PathVariable Long ventaId) {
        Venta venta = ventaService.findById(ventaId);
        if (venta == null) {
            return ResponseEntity.notFound().build();
        }

        List<EntityModel<DetalleVenta>> detalles = detalleVentaService.findByVentaId(ventaId).stream()
                .map(this::toDetalleModel)
                .toList();
        return ResponseEntity.ok(CollectionModel.of(detalles,
                linkTo(methodOn(VentaController.class).listarDetallesDeVenta(ventaId)).withSelfRel(),
                linkTo(methodOn(VentaController.class).obtenerVentaPorId(ventaId)).withRel("venta")));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Registrar venta",
            description = "Valida productos y stock, descuenta inventario, registra movimientos y detalles en una transacción.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venta registrada",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Venta.class),
                            examples = @ExampleObject(value = ApiExamples.VENTA_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario o producto no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content)
    })
    public ResponseEntity<EntityModel<Venta>> guardarVenta(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Detalles de productos vendidos. El usuario se obtiene del token.",
                    content = @Content(schema = @Schema(implementation = VentaRequest.class),
                            examples = @ExampleObject(value = ApiExamples.VENTA_REQUEST)))
            @RequestBody VentaRequest venta) {
        Venta guardada = ventaService.registrarVenta(venta);
        return ResponseEntity
                .created(linkTo(methodOn(VentaController.class).obtenerVentaPorId(guardada.getId())).toUri())
                .body(toModel(guardada));
    }

    private EntityModel<Venta> toModel(Venta venta) {
        EntityModel<Venta> model = EntityModel.of(venta,
                linkTo(methodOn(VentaController.class).obtenerVentaPorId(venta.getId())).withSelfRel(),
                linkTo(methodOn(VentaController.class).listarVentas()).withRel("ventas"),
                linkTo(methodOn(VentaController.class).listarDetallesDeVenta(venta.getId())).withRel("detalle-ventas"),
                linkTo(methodOn(InventarioController.class).listarMovimientosDeInventario()).withRel("inventario"));

        if (venta.getUsuario() != null && venta.getUsuario().getId() != null) {
            model.add(linkTo(methodOn(UsuarioController.class)
                    .obtenerUsuarioPorId(venta.getUsuario().getId())).withRel("usuario"));
        }
        return model;
    }

    private EntityModel<DetalleVenta> toDetalleModel(DetalleVenta detalle) {
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
