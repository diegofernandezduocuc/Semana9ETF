package com.minimarket.controller;

import com.minimarket.entity.Categoria;
import com.minimarket.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/categorias", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Categorías", description = "Clasificación de los productos.")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías", description = "Obtiene las categorías disponibles para clasificar productos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorías obtenidas correctamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Categoria.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content)
    })
    public List<Categoria> listarCategorias() {
        return categoriaService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría", description = "Obtiene una categoría por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Categoria.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content)
    })
    public ResponseEntity<Categoria> obtenerCategoriaPorId(
            @Parameter(description = "Identificador de la categoría", example = "1")
            @PathVariable Long id) {
        Categoria categoria = categoriaService.findById(id);
        return categoria == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(categoria);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear categoría", description = "Crea una categoría. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Categoria.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "409", description = "Nombre de categoría duplicado", content = @Content)
    })
    public ResponseEntity<Categoria> guardarCategoria(@RequestBody Categoria categoria) {
        Categoria guardada = categoriaService.save(categoria);
        return ResponseEntity
                .created(URI.create("/api/categorias/" + guardada.getId()))
                .body(guardada);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar categoría", description = "Actualiza una categoría existente. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Categoria.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Nombre de categoría duplicado", content = @Content)
    })
    public ResponseEntity<Categoria> actualizarCategoria(
            @Parameter(description = "Identificador de la categoría", example = "1")
            @PathVariable Long id,
            @RequestBody Categoria categoria) {
        if (categoriaService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        categoria.setId(id);
        return ResponseEntity.ok(categoriaService.save(categoria));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría", description = "Elimina una categoría existente. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La categoría posee productos relacionados", content = @Content)
    })
    public ResponseEntity<Void> eliminarCategoria(
            @Parameter(description = "Identificador de la categoría", example = "1")
            @PathVariable Long id) {
        if (categoriaService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        categoriaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
