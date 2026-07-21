package com.minimarket.dto;

public class DisponibilidadSucursalResponse {
    private Long sucursalId;
    private String sucursal;
    private Long productoId;
    private String producto;
    private Integer stockActual;
    private Integer stockMinimo;
    private Boolean disponible;

    public DisponibilidadSucursalResponse(Long sucursalId, String sucursal, Long productoId, String producto,
                                           Integer stockActual, Integer stockMinimo) {
        this.sucursalId = sucursalId;
        this.sucursal = sucursal;
        this.productoId = productoId;
        this.producto = producto;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.disponible = stockActual != null && stockActual > 0;
    }

    public Long getSucursalId() { return sucursalId; }
    public String getSucursal() { return sucursal; }
    public Long getProductoId() { return productoId; }
    public String getProducto() { return producto; }
    public Integer getStockActual() { return stockActual; }
    public Integer getStockMinimo() { return stockMinimo; }
    public Boolean getDisponible() { return disponible; }
}
