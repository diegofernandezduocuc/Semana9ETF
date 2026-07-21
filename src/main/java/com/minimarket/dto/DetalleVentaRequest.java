package com.minimarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producto y cantidad solicitados en una venta")
public class DetalleVentaRequest {

    @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productoId;

    @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer cantidad;

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
}
