package com.minimarket.service.impl;

import com.minimarket.dto.InventarioRequest;
import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.service.InventarioService;
import com.minimarket.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final StockService stockService;

    public InventarioServiceImpl(
            InventarioRepository inventarioRepository,
            StockService stockService) {
        this.inventarioRepository = inventarioRepository;
        this.stockService = stockService;
    }

    @Override
    public List<Inventario> findAll() {
        return inventarioRepository.findAll();
    }

    @Override
    public Inventario findById(Long id) {
        return inventarioRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Inventario registrarMovimiento(InventarioRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Debe ingresar datos del movimiento");
        }
        if (request.getProductoId() == null) {
            throw new IllegalArgumentException("Debe indicar producto");
        }

        Producto producto = new Producto();
        producto.setId(request.getProductoId());
        return stockService.registrarMovimiento(producto, request.getCantidad(), request.getTipoMovimiento());
    }

    @Override
    public List<Inventario> findByProductoId(Long productoId) {
        return inventarioRepository.findByProductoId(productoId);
    }
}
