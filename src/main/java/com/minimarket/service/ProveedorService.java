package com.minimarket.service;

import com.minimarket.entity.Proveedor;
import com.minimarket.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProveedorService {
    private final ProveedorRepository repository;
    public ProveedorService(ProveedorRepository repository) { this.repository = repository; }
    public List<Proveedor> findAll() { return repository.findAll(); }
    public Proveedor findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado")); }
    public Proveedor save(Proveedor proveedor) {
        if (proveedor == null || proveedor.getNombre() == null || proveedor.getNombre().isBlank()
                || proveedor.getCorreo() == null || !proveedor.getCorreo().contains("@")) {
            throw new IllegalArgumentException("Nombre y correo válido del proveedor son obligatorios");
        }
        proveedor.setCorreo(proveedor.getCorreo().trim().toLowerCase());
        if (proveedor.getActivo() == null) proveedor.setActivo(true);
        return repository.save(proveedor);
    }
    public Proveedor update(Long id, Proveedor datos) {
        Proveedor actual = findById(id);
        actual.setNombre(datos.getNombre());
        actual.setCorreo(datos.getCorreo());
        actual.setTelefono(datos.getTelefono());
        actual.setActivo(datos.getActivo() == null ? actual.getActivo() : datos.getActivo());
        return save(actual);
    }
    public void delete(Long id) { repository.delete(findById(id)); }
}
