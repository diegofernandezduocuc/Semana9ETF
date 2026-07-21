package com.minimarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Movimiento de stock para una sucursal.")
public class StockSucursalRequest {
    private Long sucursalId;
    private Long productoId;
    private Integer cantidad;
    private String tipoMovimiento;
    private Integer stockMinimo;

    public Long getSucursalId() { return sucursalId; }
    public void setSucursalId(Long sucursalId) { this.sucursalId = sucursalId; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }
}
