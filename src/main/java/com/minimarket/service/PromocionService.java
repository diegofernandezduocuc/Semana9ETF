package com.minimarket.service;

import com.minimarket.dto.PromocionRequest;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Promocion;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.PromocionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PromocionService {
    private final PromocionRepository repository;
    private final ProductoRepository productoRepository;

    public PromocionService(PromocionRepository repository, ProductoRepository productoRepository) {
        this.repository = repository;
        this.productoRepository = productoRepository;
    }

    public List<Promocion> findAll() { return repository.findAll(); }
    public List<Promocion> findActivas() {
        LocalDate hoy = LocalDate.now();
        return repository.findByActivaTrue().stream().filter(p -> vigente(p, hoy)).toList();
    }
    public Promocion findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada")); }

    public Promocion save(PromocionRequest request, Long id) {
        validar(request);
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        Promocion promocion = id == null ? new Promocion() : findById(id);
        promocion.setNombre(request.getNombre().trim());
        promocion.setProducto(producto);
        promocion.setPorcentajeDescuento(request.getPorcentajeDescuento());
        promocion.setFechaInicio(request.getFechaInicio());
        promocion.setFechaFin(request.getFechaFin());
        promocion.setActiva(request.getActiva() == null ? true : request.getActiva());
        return repository.save(promocion);
    }

    public void delete(Long id) { repository.delete(findById(id)); }

    public double precioVigente(Producto producto) {
        LocalDate hoy = LocalDate.now();
        double descuento = repository.findByProductoIdAndActivaTrue(producto.getId()).stream()
                .filter(p -> vigente(p, hoy))
                .map(Promocion::getPorcentajeDescuento)
                .max(Comparator.naturalOrder())
                .orElse(0.0);
        return producto.getPrecio() * (1.0 - descuento / 100.0);
    }

    private boolean vigente(Promocion promocion, LocalDate fecha) {
        return Boolean.TRUE.equals(promocion.getActiva())
                && !fecha.isBefore(promocion.getFechaInicio())
                && !fecha.isAfter(promocion.getFechaFin());
    }

    private void validar(PromocionRequest request) {
        if (request == null || request.getNombre() == null || request.getNombre().isBlank()
                || request.getProductoId() == null || request.getPorcentajeDescuento() == null
                || request.getFechaInicio() == null || request.getFechaFin() == null) {
            throw new IllegalArgumentException("Datos de promoción incompletos");
        }
        if (request.getPorcentajeDescuento() <= 0 || request.getPorcentajeDescuento() >= 100) {
            throw new IllegalArgumentException("El descuento debe ser mayor que 0 y menor que 100");
        }
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
    }
}
