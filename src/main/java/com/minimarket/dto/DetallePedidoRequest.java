package com.minimarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producto y cantidad solicitados en un pedido.")
public class DetallePedidoRequest {
    private Long productoId;
    private Integer cantidad;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
