package com.minimarket.dto;

public class RotacionProductoResponse {
    private Long productoId;
    private String producto;
    private Integer unidadesVendidas;
    private Integer unidadesPedidas;
    private Integer totalMovido;

    public RotacionProductoResponse(Long productoId, String producto, Integer unidadesVendidas, Integer unidadesPedidas) {
        this.productoId = productoId;
        this.producto = producto;
        this.unidadesVendidas = unidadesVendidas;
        this.unidadesPedidas = unidadesPedidas;
        this.totalMovido = unidadesVendidas + unidadesPedidas;
    }

    public Long getProductoId() { return productoId; }
    public String getProducto() { return producto; }
    public Integer getUnidadesVendidas() { return unidadesVendidas; }
    public Integer getUnidadesPedidas() { return unidadesPedidas; }
    public Integer getTotalMovido() { return totalMovido; }
}
