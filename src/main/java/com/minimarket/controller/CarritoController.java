package com.minimarket.controller;

import com.minimarket.config.ApiExamples;
import com.minimarket.entity.Carrito;
import com.minimarket.service.CarritoService;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/carrito", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Carrito", description = "Operaciones para administrar productos del carrito con control de propietario.")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    @Operation(summary = "Listar carrito",
            description = "ROLE_CLIENTE obtiene solo sus registros; ROLE_ADMIN obtiene todos los carritos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito obtenido correctamente",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            examples = @ExampleObject(value = ApiExamples.CARRITO_COLLECTION))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public CollectionModel<EntityModel<Carrito>> listarCarrito() {
        List<EntityModel<Carrito>> registros = carritoService.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(registros,
                linkTo(methodOn(CarritoController.class).listarCarrito()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener registro del carrito",
            description = "ROLE_CLIENTE solo puede obtener registros propios; ROLE_ADMIN puede obtener cualquiera.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Carrito.class),
                            examples = @ExampleObject(value = ApiExamples.CARRITO_RESPONSE))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El carrito pertenece a otro usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado", content = @Content)
    })
    public ResponseEntity<EntityModel<Carrito>> obtenerCarritoPorId(
            @Parameter(description = "Identificador del registro", example = "1") @PathVariable Long id) {
        Carrito carrito = carritoService.findById(id);
        return carrito == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(toModel(carrito));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Agregar producto al carrito",
            description = "ROLE_CLIENTE crea registros para su propio usuario autenticado; ROLE_ADMIN puede indicar usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto agregado al carrito",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            examples = @ExampleObject(value = ApiExamples.CARRITO_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public ResponseEntity<EntityModel<Carrito>> agregarProductoAlCarrito(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Usuario, producto y cantidad",
                    content = @Content(schema = @Schema(implementation = Carrito.class),
                            examples = @ExampleObject(value = ApiExamples.CARRITO_REQUEST)))
            @RequestBody Carrito carrito) {
        Carrito guardado = carritoService.save(carrito);
        return ResponseEntity
                .created(linkTo(methodOn(CarritoController.class).obtenerCarritoPorId(guardado.getId())).toUri())
                .body(toModel(guardado));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Actualizar registro del carrito",
            description = "ROLE_CLIENTE solo puede modificar registros propios; ROLE_ADMIN puede modificar cualquiera.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El carrito pertenece a otro usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado", content = @Content)
    })
    public ResponseEntity<EntityModel<Carrito>> actualizarCarrito(
            @Parameter(description = "Identificador del registro", example = "1") @PathVariable Long id,
            @RequestBody Carrito carrito) {
        if (carritoService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        carrito.setId(id);
        return ResponseEntity.ok(toModel(carritoService.save(carrito)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto del carrito",
            description = "ROLE_CLIENTE solo puede eliminar registros propios; ROLE_ADMIN puede eliminar cualquiera.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro eliminado"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El carrito pertenece a otro usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminarProductoDelCarrito(
            @Parameter(description = "Identificador del registro", example = "1") @PathVariable Long id) {
        if (carritoService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        carritoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<Carrito> toModel(Carrito carrito) {
        EntityModel<Carrito> model = EntityModel.of(carrito,
                linkTo(methodOn(CarritoController.class).obtenerCarritoPorId(carrito.getId())).withSelfRel(),
                linkTo(methodOn(CarritoController.class).listarCarrito()).withRel("carrito"));

        if (carrito.getUsuario() != null && carrito.getUsuario().getId() != null) {
            model.add(linkTo(methodOn(UsuarioController.class)
                    .obtenerUsuarioPorId(carrito.getUsuario().getId())).withRel("usuario"));
        }
        if (carrito.getProducto() != null && carrito.getProducto().getId() != null) {
            model.add(linkTo(methodOn(ProductoController.class)
                    .obtenerProductoPorId(carrito.getProducto().getId())).withRel("producto"));
        }
        return model;
    }
}
