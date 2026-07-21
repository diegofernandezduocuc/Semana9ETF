package com.minimarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Solicitud de pedido en línea.")
public class PedidoRequest {
    private Long sucursalId;
    private String tipoEntrega;
    private String direccionDespacho;
    private List<DetallePedidoRequest> detalles;

    public Long getSucursalId() { return sucursalId; }
    public void setSucursalId(Long sucursalId) { this.sucursalId = sucursalId; }
    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { this.tipoEntrega = tipoEntrega; }
    public String getDireccionDespacho() { return direccionDespacho; }
    public void setDireccionDespacho(String direccionDespacho) { this.direccionDespacho = direccionDespacho; }
    public List<DetallePedidoRequest> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedidoRequest> detalles) { this.detalles = detalles; }
}
