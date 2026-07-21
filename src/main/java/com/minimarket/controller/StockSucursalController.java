package com.minimarket.controller;

import com.minimarket.dto.DisponibilidadSucursalResponse;
import com.minimarket.dto.StockSucursalRequest;
import com.minimarket.entity.StockSucursal;
import com.minimarket.service.StockSucursalService;
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
@RequestMapping(value = "/api/stock-sucursales", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Stock por sucursal", description = "Disponibilidad y movimientos de stock por sucursal.")
public class StockSucursalController {

    private final StockSucursalService service;

    public StockSucursalController(StockSucursalService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar stock por sucursal", description = "Obtiene todas las existencias configuradas por sucursal y producto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock obtenido correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<StockSucursal>> listar() {
        return collection(service.findAll(), null);
    }

    @GetMapping("/sucursal/{sucursalId}")
    @Operation(summary = "Listar stock de una sucursal", description = "Obtiene las existencias de todos los productos en una sucursal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock de la sucursal obtenido correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada", content = @Content)
    })
    public CollectionModel<EntityModel<StockSucursal>> listarPorSucursal(
            @Parameter(description = "Identificador de la sucursal", example = "1")
            @PathVariable Long sucursalId) {
        return collection(service.findBySucursal(sucursalId), sucursalId);
    }

    @GetMapping("/{sucursalId}/{productoId}/disponibilidad")
    @Operation(
            summary = "Consultar disponibilidad de un producto en una sucursal",
            description = "Informa stock actual, stock mínimo y disponibilidad del producto solicitado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad obtenida",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = DisponibilidadSucursalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe stock del producto en la sucursal", content = @Content)
    })
    public DisponibilidadSucursalResponse disponibilidad(
            @Parameter(description = "Identificador de la sucursal", example = "1")
            @PathVariable Long sucursalId,
            @Parameter(description = "Identificador del producto", example = "1")
            @PathVariable Long productoId) {
        return service.disponibilidad(sucursalId, productoId);
    }

    @PostMapping(consumes = "application/json")
    @Operation(
            summary = "Registrar movimiento de stock por sucursal",
            description = "Registra una ENTRADA o SALIDA. Una salida que alcanza el mínimo puede generar una orden automática. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimiento registrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = StockSucursal.class))),
            @ApiResponse(responseCode = "400", description = "Datos, cantidad o tipo de movimiento inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sucursal o producto no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content)
    })
    public ResponseEntity<EntityModel<StockSucursal>> registrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Sucursal, producto, cantidad, tipo de movimiento y stock mínimo opcional",
                    content = @Content(schema = @Schema(implementation = StockSucursalRequest.class)))
            @RequestBody StockSucursalRequest request) {
        StockSucursal guardado = service.registrarMovimiento(request);
        return ResponseEntity.status(201).body(toModel(guardado));
    }

    private CollectionModel<EntityModel<StockSucursal>> collection(List<StockSucursal> stocks, Long sucursalId) {
        CollectionModel<EntityModel<StockSucursal>> model = CollectionModel.of(
                stocks.stream().map(this::toModel).toList(),
                linkTo(methodOn(StockSucursalController.class).listar()).withRel("stock-sucursales"));
        if (sucursalId == null) {
            model.add(linkTo(methodOn(StockSucursalController.class).listar()).withSelfRel());
        } else {
            model.add(linkTo(methodOn(StockSucursalController.class)
                    .listarPorSucursal(sucursalId)).withSelfRel());
            model.add(linkTo(methodOn(SucursalController.class).obtener(sucursalId)).withRel("sucursal"));
        }
        return model;
    }

    private EntityModel<StockSucursal> toModel(StockSucursal stock) {
        return EntityModel.of(stock,
                linkTo(methodOn(StockSucursalController.class)
                        .disponibilidad(stock.getSucursal().getId(), stock.getProducto().getId())).withRel("disponibilidad"),
                linkTo(methodOn(StockSucursalController.class).listar()).withRel("stock-sucursales"),
                linkTo(methodOn(SucursalController.class).obtener(stock.getSucursal().getId())).withRel("sucursal"),
                linkTo(methodOn(ProductoController.class)
                        .obtenerProductoPorId(stock.getProducto().getId())).withRel("producto"));
    }
}
