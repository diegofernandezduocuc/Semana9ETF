package com.minimarket.service;

import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;

@Service
public class StockService {

    public static final String ENTRADA = "ENTRADA";
    public static final String SALIDA = "SALIDA";

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;

    public StockService(
            ProductoRepository productoRepository,
            InventarioRepository inventarioRepository) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    public Inventario registrarMovimiento(Inventario inventario) {
        if (inventario == null) {
            throw new IllegalArgumentException("Debe ingresar datos del movimiento");
        }
        Producto producto = resolverProducto(inventario.getProducto());
        int cantidad = validarCantidad(inventario.getCantidad());
        String tipoMovimiento = normalizarTipoMovimiento(inventario.getTipoMovimiento());

        aplicarMovimiento(producto, cantidad, tipoMovimiento);
        inventario.setProducto(productoRepository.save(producto));
        inventario.setCantidad(cantidad);
        inventario.setTipoMovimiento(tipoMovimiento);
        if (inventario.getFechaMovimiento() == null) {
            inventario.setFechaMovimiento(new Date());
        }
        return inventarioRepository.save(inventario);
    }

    public Inventario registrarMovimiento(Producto producto, Integer cantidad, String tipoMovimiento) {
        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setCantidad(cantidad);
        inventario.setTipoMovimiento(tipoMovimiento);
        inventario.setFechaMovimiento(new Date());
        return registrarMovimiento(inventario);
    }

    public Producto resolverProducto(Producto producto) {
        if (producto == null || producto.getId() == null) {
            throw new IllegalArgumentException("Debe indicar producto");
        }
        return productoRepository.findByIdForUpdate(producto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    public int validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        return cantidad;
    }

    public void validarStockDisponible(Producto producto, int cantidad) {
        int stockActual = producto.getStock() == null ? 0 : producto.getStock();
        if (stockActual < cantidad) {
            throw new StockInsuficienteException("Stock insuficiente para el producto " + producto.getId());
        }
    }

    private void aplicarMovimiento(Producto producto, int cantidad, String tipoMovimiento) {
        int stockActual = producto.getStock() == null ? 0 : producto.getStock();
        if (ENTRADA.equals(tipoMovimiento)) {
            producto.setStock(stockActual + cantidad);
            return;
        }
        validarStockDisponible(producto, cantidad);
        producto.setStock(stockActual - cantidad);
    }

    private String normalizarTipoMovimiento(String tipoMovimiento) {
        if (tipoMovimiento == null || tipoMovimiento.isBlank()) {
            throw new IllegalArgumentException("Debe indicar tipo de movimiento");
        }
        String normalizado = tipoMovimiento.trim().toUpperCase(Locale.ROOT);
        if (!ENTRADA.equals(normalizado) && !SALIDA.equals(normalizado)) {
            throw new IllegalArgumentException("El tipo de movimiento debe ser ENTRADA o SALIDA");
        }
        return normalizado;
    }
}
