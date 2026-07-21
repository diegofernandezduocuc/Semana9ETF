package com.minimarket.controller;

import com.minimarket.dto.RotacionProductoResponse;
import com.minimarket.entity.OrdenCompra;
import com.minimarket.entity.StockSucursal;
import com.minimarket.service.OrdenCompraService;
import com.minimarket.service.ReporteService;
import com.minimarket.service.StockSucursalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/reportes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reportes", description = "Reportes administrativos de rotación, stock y reposición.")
public class ReporteController {

    private final ReporteService reporteService;
    private final StockSucursalService stockService;
    private final OrdenCompraService ordenCompraService;

    public ReporteController(
            ReporteService reporteService,
            StockSucursalService stockService,
            OrdenCompraService ordenCompraService) {
        this.reporteService = reporteService;
        this.stockService = stockService;
        this.ordenCompraService = ordenCompraService;
    }

    @GetMapping("/rotacion-productos")
    @Operation(
            summary = "Generar reporte de rotación de productos",
            description = "Combina ventas y pedidos para ordenar los productos según el total de unidades movidas. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = RotacionProductoResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content)
    })
    public List<RotacionProductoResponse> rotacion() {
        return reporteService.rotacion();
    }

    @GetMapping("/stock-sucursales")
    @Operation(
            summary = "Generar reporte de stock por sucursal",
            description = "Obtiene las existencias y mínimos configurados para cada sucursal y producto. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = StockSucursal.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content)
    })
    public List<StockSucursal> stock() {
        return stockService.findAll();
    }

    @GetMapping("/ordenes-pendientes")
    @Operation(
            summary = "Generar reporte de órdenes pendientes",
            description = "Obtiene las órdenes de reposición pendientes de recepción o cancelación. Requiere ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = OrdenCompra.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content)
    })
    public List<OrdenCompra> ordenesPendientes() {
        return ordenCompraService.findPendientes();
    }
}
