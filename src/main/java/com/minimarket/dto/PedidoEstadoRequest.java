package com.minimarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cambio de estado administrativo de un pedido.")
public class PedidoEstadoRequest {
    private String estado;
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
