package com.minimarket.service;

import com.minimarket.entity.OrdenCompra;
import com.minimarket.repository.OrdenCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrdenCompraService {
    private final OrdenCompraRepository repository;
    private final StockSucursalService stockSucursalService;

    public OrdenCompraService(OrdenCompraRepository repository, StockSucursalService stockSucursalService) {
        this.repository = repository;
        this.stockSucursalService = stockSucursalService;
    }

    public List<OrdenCompra> findAll() { return repository.findAll(); }
    public List<OrdenCompra> findPendientes() { return repository.findByEstado("PENDIENTE"); }
    public OrdenCompra findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada")); }

    @Transactional
    public OrdenCompra recibir(Long id) {
        OrdenCompra orden = findById(id);
        if (!"PENDIENTE".equals(orden.getEstado())) throw new IllegalArgumentException("La orden no está pendiente");
        stockSucursalService.reponerDesdeOrden(orden);
        orden.setEstado("RECIBIDA");
        return repository.save(orden);
    }

    public OrdenCompra cancelar(Long id) {
        OrdenCompra orden = findById(id);
        if (!"PENDIENTE".equals(orden.getEstado())) throw new IllegalArgumentException("La orden no está pendiente");
        orden.setEstado("CANCELADA");
        return repository.save(orden);
    }
}
