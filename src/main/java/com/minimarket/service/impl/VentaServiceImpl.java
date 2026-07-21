package com.minimarket.service.impl;

import com.minimarket.dto.DetalleVentaRequest;
import com.minimarket.dto.VentaRequest;
import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
import com.minimarket.service.ResourceNotFoundException;
import com.minimarket.service.StockService;
import com.minimarket.service.VentaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final StockService stockService;

    public VentaServiceImpl(
            VentaRepository ventaRepository,
            UsuarioRepository usuarioRepository,
            StockService stockService) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.stockService = stockService;
    }

    @Override
    public List<Venta> findAll() {
        return ventaRepository.findAll();
    }

    @Override
    public Venta findById(Long id) {
        return ventaRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Venta registrarVenta(VentaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Debe ingresar datos de la venta");
        }

        Usuario usuario = usuarioAutenticado();
        List<DetalleVentaRequest> detalles = validarDetalles(request.getDetalles());
        Map<Long, Producto> productos = resolverProductos(detalles);
        Map<Long, Integer> cantidadesPorProducto = sumarCantidadesPorProducto(detalles);
        validarStockDisponible(productos, cantidadesPorProducto);

        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        Venta guardada = ventaRepository.save(venta);
        List<DetalleVenta> detallesProcesados = new ArrayList<>();
        for (DetalleVentaRequest detalle : detalles) {
            Producto producto = productos.get(detalle.getProductoId());
            int cantidad = detalle.getCantidad();
            stockService.registrarMovimiento(producto, cantidad, StockService.SALIDA);

            DetalleVenta procesado = new DetalleVenta();
            procesado.setVenta(guardada);
            procesado.setProducto(producto);
            procesado.setCantidad(cantidad);
            procesado.setPrecio(producto.getPrecio() * cantidad);
            detallesProcesados.add(procesado);
        }

        guardada.setDetalles(detallesProcesados);
        return ventaRepository.save(guardada);
    }

    @Override
    @Transactional
    public Venta save(Venta venta) {
        if (venta == null) {
            throw new IllegalArgumentException("Debe ingresar datos de la venta");
        }
        VentaRequest request = new VentaRequest();
        List<DetalleVentaRequest> detalles = venta.getDetalles() == null
                ? null
                : venta.getDetalles().stream()
                .map(this::toRequest)
                .toList();
        request.setDetalles(detalles);
        return registrarVenta(request);
    }

    @Override
    public List<Venta> findByUsuarioId(Long usuarioId) {
        return ventaRepository.findByUsuarioId(usuarioId);
    }

    private Usuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado"));
    }

    private List<DetalleVentaRequest> validarDetalles(List<DetalleVentaRequest> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La venta debe incluir al menos un detalle");
        }
        for (DetalleVentaRequest detalle : detalles) {
            if (detalle == null) {
                throw new IllegalArgumentException("Detalle de venta inválido");
            }
            if (detalle.getProductoId() == null) {
                throw new IllegalArgumentException("Debe indicar producto en cada detalle");
            }
            detalle.setCantidad(stockService.validarCantidad(detalle.getCantidad()));
        }
        return detalles;
    }

    private Map<Long, Producto> resolverProductos(List<DetalleVentaRequest> detalles) {
        Map<Long, Producto> productos = new LinkedHashMap<>();
        for (DetalleVentaRequest detalle : detalles) {
            Long productoId = detalle.getProductoId();
            if (!productos.containsKey(productoId)) {
                Producto producto = new Producto();
                producto.setId(productoId);
                producto = stockService.resolverProducto(producto);
                if (producto.getPrecio() == null) {
                    throw new IllegalArgumentException("El producto no tiene precio configurado");
                }
                productos.put(productoId, producto);
            }
        }
        return productos;
    }

    private Map<Long, Integer> sumarCantidadesPorProducto(List<DetalleVentaRequest> detalles) {
        Map<Long, Integer> cantidadesPorProducto = new LinkedHashMap<>();
        for (DetalleVentaRequest detalle : detalles) {
            cantidadesPorProducto.merge(detalle.getProductoId(), detalle.getCantidad(), Integer::sum);
        }
        return cantidadesPorProducto;
    }

    private void validarStockDisponible(
            Map<Long, Producto> productos,
            Map<Long, Integer> cantidadesPorProducto) {
        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            stockService.validarStockDisponible(productos.get(entry.getKey()), entry.getValue());
        }
    }

    private DetalleVentaRequest toRequest(DetalleVenta detalle) {
        DetalleVentaRequest request = new DetalleVentaRequest();
        if (detalle.getProducto() != null) {
            request.setProductoId(detalle.getProducto().getId());
        }
        request.setCantidad(detalle.getCantidad());
        return request;
    }
}
