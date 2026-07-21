package com.minimarket.controller;

import com.minimarket.config.ApiExamples;
import com.minimarket.entity.Producto;
import com.minimarket.service.ProductoService;
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
@RequestMapping(value = "/api/productos", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Productos", description = "Gestión completa del catálogo de productos.")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Obtiene todos los productos con enlaces HATEOAS.")
    @ApiResponse(responseCode = "200", description = "Productos obtenidos correctamente",
            content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                    examples = @ExampleObject(value = ApiExamples.PRODUCTOS_COLLECTION)))
    public CollectionModel<EntityModel<Producto>> listarProductos() {
        List<EntityModel<Producto>> productos = productoService.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(productos,
                linkTo(methodOn(ProductoController.class).listarProductos()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto", description = "Busca un producto por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Producto.class),
                            examples = @ExampleObject(value = ApiExamples.PRODUCTO_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content)
    })
    public ResponseEntity<EntityModel<Producto>> obtenerProductoPorId(
            @Parameter(description = "Identificador del producto", example = "1") @PathVariable Long id) {
        Producto producto = productoService.findById(id);
        return producto == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(toModel(producto));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Crear producto", description = "Agrega un producto al catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado",
                    content = @Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            examples = @ExampleObject(value = ApiExamples.PRODUCTO_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Datos del producto inválidos", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol administrador", content = @Content)
    })
    public ResponseEntity<EntityModel<Producto>> guardarProducto(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos del nuevo producto",
                    content = @Content(schema = @Schema(implementation = Producto.class),
                            examples = @ExampleObject(value = ApiExamples.PRODUCTO_REQUEST)))
            @RequestBody Producto producto) {
        Producto guardado = productoService.save(producto);
        return ResponseEntity
                .created(linkTo(methodOn(ProductoController.class).obtenerProductoPorId(guardado.getId())).toUri())
                .body(toModel(guardado));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Actualizar producto", description = "Reemplaza los datos de un producto existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol administrador", content = @Content)
    })
    public ResponseEntity<EntityModel<Producto>> actualizarProducto(
            @Parameter(description = "Identificador del producto", example = "1") @PathVariable Long id,
            @RequestBody Producto producto) {
        if (productoService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        producto.setId(id);
        return ResponseEntity.ok(toModel(productoService.save(producto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto", description = "Elimina un producto del catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol administrador", content = @Content)
    })
    public ResponseEntity<Void> eliminarProducto(
            @Parameter(description = "Identificador del producto", example = "1") @PathVariable Long id) {
        if (productoService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        productoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<Producto> toModel(Producto producto) {
        EntityModel<Producto> model = EntityModel.of(producto,
                linkTo(methodOn(ProductoController.class).obtenerProductoPorId(producto.getId())).withSelfRel(),
                linkTo(methodOn(ProductoController.class).listarProductos()).withRel("productos"),
                linkTo(methodOn(InventarioController.class).listarMovimientosDeInventario()).withRel("inventario"));

        if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
            model.add(linkTo(methodOn(CategoriaController.class)
                    .obtenerCategoriaPorId(producto.getCategoria().getId())).withRel("categoria"));
        }
        return model;
    }
}
