package com.minimarket.service;

import com.minimarket.entity.Sucursal;
import com.minimarket.repository.SucursalRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SucursalService {
    private final SucursalRepository repository;
    public SucursalService(SucursalRepository repository) { this.repository = repository; }
    public List<Sucursal> findAll() { return repository.findAll(); }
    public Sucursal findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada")); }
    public Sucursal save(Sucursal sucursal) {
        if (sucursal == null || sucursal.getNombre() == null || sucursal.getNombre().isBlank()
                || sucursal.getDireccion() == null || sucursal.getDireccion().isBlank()) {
            throw new IllegalArgumentException("Nombre y dirección de sucursal son obligatorios");
        }
        if (sucursal.getActiva() == null) sucursal.setActiva(true);
        return repository.save(sucursal);
    }
    public Sucursal update(Long id, Sucursal datos) {
        Sucursal actual = findById(id);
        actual.setNombre(datos.getNombre());
        actual.setDireccion(datos.getDireccion());
        actual.setActiva(datos.getActiva() == null ? actual.getActiva() : datos.getActiva());
        return save(actual);
    }
    public void delete(Long id) { repository.delete(findById(id)); }
}
