package com.minimarket.controller;

import com.minimarket.dto.PedidoEstadoRequest;
import com.minimarket.dto.PedidoRequest;
import com.minimarket.entity.DetallePedido;
import com.minimarket.entity.Pedido;
import com.minimarket.service.PedidoService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/pedidos", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Pedidos en línea", description = "Pedidos para retiro en tienda o despacho a domicilio.")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Listar pedidos",
            description = "ROLE_CLIENTE obtiene sus pedidos; ROLE_ADMIN obtiene todos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos obtenidos correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<Pedido>> listar() {
        return collection(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener pedido",
            description = "ROLE_CLIENTE solo puede consultar pedidos propios; ROLE_ADMIN puede consultar cualquiera."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El pedido pertenece a otro usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    public EntityModel<Pedido> obtener(
            @Parameter(description = "Identificador del pedido", example = "1")
            @PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @GetMapping("/{id}/detalles")
    @Operation(
            summary = "Listar detalles de un pedido",
            description = "Obtiene los productos y precios asociados a un pedido accesible para el usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalles obtenidos correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El pedido pertenece a otro usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    public CollectionModel<EntityModel<DetallePedido>> detalles(
            @Parameter(description = "Identificador del pedido", example = "1")
            @PathVariable Long id) {
        List<EntityModel<DetallePedido>> items = service.findDetalles(id).stream()
                .map(detalle -> EntityModel.of(detalle,
                        linkTo(methodOn(PedidoController.class).detalles(id)).withRel("pedido"),
                        linkTo(methodOn(ProductoController.class)
                                .obtenerProductoPorId(detalle.getProducto().getId())).withRel("producto")))
                .toList();
        return CollectionModel.of(items,
                linkTo(methodOn(PedidoController.class).detalles(id)).withSelfRel(),
                linkTo(methodOn(PedidoController.class).obtener(id)).withRel("pedido"));
    }

    @PostMapping(consumes = "application/json")
    @Operation(
            summary = "Crear pedido",
            description = "Crea un pedido para retiro o despacho, aplica promociones vigentes y descuenta stock por sucursal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "400", description = "Datos, cantidades o dirección inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sucursal, producto o stock no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content)
    })
    public ResponseEntity<EntityModel<Pedido>> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Sucursal, tipo de entrega, dirección cuando corresponda y detalles del pedido",
                    content = @Content(schema = @Schema(implementation = PedidoRequest.class)))
            @RequestBody PedidoRequest request) {
        Pedido guardado = service.registrar(request);
        return ResponseEntity
                .created(linkTo(methodOn(PedidoController.class).obtener(guardado.getId())).toUri())
                .body(toModel(guardado));
    }

    @PutMapping(value = "/{id}/estado", consumes = "application/json")
    @Operation(
            summary = "Actualizar estado de pedido",
            description = "Actualiza el estado operativo de un pedido. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    public EntityModel<Pedido> estado(
            @Parameter(description = "Identificador del pedido", example = "1")
            @PathVariable Long id,
            @RequestBody PedidoEstadoRequest request) {
        return toModel(service.actualizarEstado(id, request.getEstado()));
    }

    private CollectionModel<EntityModel<Pedido>> collection(List<Pedido> pedidos) {
        return CollectionModel.of(pedidos.stream().map(this::toModel).toList(),
                linkTo(methodOn(PedidoController.class).listar()).withSelfRel());
    }

    private EntityModel<Pedido> toModel(Pedido pedido) {
        return EntityModel.of(pedido,
                linkTo(methodOn(PedidoController.class).obtener(pedido.getId())).withSelfRel(),
                linkTo(methodOn(PedidoController.class).listar()).withRel("pedidos"),
                linkTo(methodOn(PedidoController.class).detalles(pedido.getId())).withRel("detalles"),
                linkTo(methodOn(SucursalController.class).obtener(pedido.getSucursal().getId())).withRel("sucursal"));
    }
}
