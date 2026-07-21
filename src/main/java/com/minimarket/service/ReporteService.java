package com.minimarket.service;

import com.minimarket.dto.RotacionProductoResponse;
import com.minimarket.entity.DetallePedido;
import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.repository.DetallePedidoRepository;
import com.minimarket.repository.DetalleVentaRepository;
import com.minimarket.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {
    private final ProductoRepository productoRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public ReporteService(ProductoRepository productoRepository, DetalleVentaRepository detalleVentaRepository,
                          DetallePedidoRepository detallePedidoRepository) {
        this.productoRepository = productoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    public List<RotacionProductoResponse> rotacion() {
        Map<Long, Integer> ventas = new LinkedHashMap<>();
        for (DetalleVenta detalle : detalleVentaRepository.findAll()) {
            ventas.merge(detalle.getProducto().getId(), detalle.getCantidad(), Integer::sum);
        }
        Map<Long, Integer> pedidos = new LinkedHashMap<>();
        for (DetallePedido detalle : detallePedidoRepository.findAll()) {
            pedidos.merge(detalle.getProducto().getId(), detalle.getCantidad(), Integer::sum);
        }
        List<RotacionProductoResponse> resultado = new ArrayList<>();
        for (Producto producto : productoRepository.findAll()) {
            resultado.add(new RotacionProductoResponse(producto.getId(), producto.getNombre(),
                    ventas.getOrDefault(producto.getId(), 0), pedidos.getOrDefault(producto.getId(), 0)));
        }
        resultado.sort(Comparator.comparing(RotacionProductoResponse::getTotalMovido).reversed()
                .thenComparing(RotacionProductoResponse::getProducto));
        return resultado;
    }
}
