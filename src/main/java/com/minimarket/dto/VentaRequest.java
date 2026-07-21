package com.minimarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Solicitud de venta. El usuario se obtiene del token autenticado.")
public class VentaRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DetalleVentaRequest> detalles;

    public List<DetalleVentaRequest> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVentaRequest> detalles) {
        this.detalles = detalles;
    }
}
