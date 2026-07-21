package com.minimarket.service;

import com.minimarket.dto.InventarioRequest;
import com.minimarket.entity.Inventario;

import java.util.List;

public interface InventarioService {
    List<Inventario> findAll();
    Inventario findById(Long id);
    Inventario registrarMovimiento(InventarioRequest request);
    List<Inventario> findByProductoId(Long productoId);
}
