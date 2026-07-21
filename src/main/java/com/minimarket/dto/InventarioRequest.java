package com.minimarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de movimiento de inventario")
public class InventarioRequest {

    @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productoId;

    @Schema(example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer cantidad;

    @Schema(example = "ENTRADA", allowableValues = {"ENTRADA", "SALIDA"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipoMovimiento;

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }
}
