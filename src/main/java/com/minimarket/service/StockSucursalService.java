package com.minimarket.service;

import com.minimarket.dto.DisponibilidadSucursalResponse;
import com.minimarket.dto.StockSucursalRequest;
import com.minimarket.entity.OrdenCompra;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Proveedor;
import com.minimarket.entity.StockSucursal;
import com.minimarket.entity.Sucursal;
import com.minimarket.repository.OrdenCompraRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.ProveedorRepository;
import com.minimarket.repository.StockSucursalRepository;
import com.minimarket.repository.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class StockSucursalService {
    public static final String ENTRADA = "ENTRADA";
    public static final String SALIDA = "SALIDA";

    private final StockSucursalRepository repository;
    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;

    public StockSucursalService(StockSucursalRepository repository, SucursalRepository sucursalRepository,
                                ProductoRepository productoRepository, OrdenCompraRepository ordenCompraRepository,
                                ProveedorRepository proveedorRepository) {
        this.repository = repository;
        this.sucursalRepository = sucursalRepository;
        this.productoRepository = productoRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.proveedorRepository = proveedorRepository;
    }

    public List<StockSucursal> findAll() { return repository.findAll(); }
    public List<StockSucursal> findBySucursal(Long sucursalId) {
        if (!sucursalRepository.existsById(sucursalId)) throw new ResourceNotFoundException("Sucursal no encontrada");
        return repository.findBySucursalId(sucursalId);
    }
    public StockSucursal findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stock de sucursal no encontrado")); }

    public DisponibilidadSucursalResponse disponibilidad(Long sucursalId, Long productoId) {
        StockSucursal stock = repository.findBySucursalIdAndProductoId(sucursalId, productoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe stock del producto en la sucursal"));
        return new DisponibilidadSucursalResponse(stock.getSucursal().getId(), stock.getSucursal().getNombre(),
                stock.getProducto().getId(), stock.getProducto().getNombre(), stock.getStockActual(), stock.getStockMinimo());
    }

    @Transactional
    public StockSucursal registrarMovimiento(StockSucursalRequest request) {
        if (request == null || request.getSucursalId() == null || request.getProductoId() == null) {
            throw new IllegalArgumentException("Sucursal y producto son obligatorios");
        }
        int cantidad = validarCantidad(request.getCantidad());
        String tipo = normalizarTipo(request.getTipoMovimiento());
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        StockSucursal stock = repository.findForUpdate(sucursal.getId(), producto.getId()).orElseGet(() -> {
            StockSucursal nuevo = new StockSucursal();
            nuevo.setSucursal(sucursal);
            nuevo.setProducto(producto);
            nuevo.setStockActual(0);
            nuevo.setStockMinimo(request.getStockMinimo() == null ? 5 : validarStockMinimo(request.getStockMinimo()));
            return repository.save(nuevo);
        });
        if (request.getStockMinimo() != null) stock.setStockMinimo(validarStockMinimo(request.getStockMinimo()));
        if (ENTRADA.equals(tipo)) {
            stock.setStockActual(stock.getStockActual() + cantidad);
        } else {
            validarStock(stock, cantidad);
            stock.setStockActual(stock.getStockActual() - cantidad);
        }
        StockSucursal guardado = repository.save(stock);
        generarOrdenAutomaticaSiCorresponde(guardado);
        return guardado;
    }

    @Transactional
    public StockSucursal obtenerParaActualizacion(Long sucursalId, Long productoId) {
        return repository.findForUpdate(sucursalId, productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto sin stock configurado en la sucursal"));
    }

    public void validarStock(StockSucursal stock, int cantidad) {
        if (stock.getStockActual() == null || stock.getStockActual() < cantidad) {
            throw new StockInsuficienteException("Stock insuficiente en la sucursal para el producto " + stock.getProducto().getId());
        }
    }

    public StockSucursal descontar(StockSucursal stock, int cantidad) {
        validarStock(stock, cantidad);
        stock.setStockActual(stock.getStockActual() - cantidad);
        StockSucursal guardado = repository.save(stock);
        generarOrdenAutomaticaSiCorresponde(guardado);
        return guardado;
    }

    public StockSucursal reponerDesdeOrden(OrdenCompra orden) {
        StockSucursal stock = obtenerParaActualizacion(orden.getSucursal().getId(), orden.getProducto().getId());
        stock.setStockActual(stock.getStockActual() + orden.getCantidad());
        return repository.save(stock);
    }

    private void generarOrdenAutomaticaSiCorresponde(StockSucursal stock) {
        if (stock.getStockActual() > stock.getStockMinimo()) return;
        if (ordenCompraRepository.existsBySucursalIdAndProductoIdAndEstado(
                stock.getSucursal().getId(), stock.getProducto().getId(), "PENDIENTE")) return;
        Proveedor proveedor = proveedorRepository.findFirstByActivoTrueOrderByIdAsc().orElse(null);
        if (proveedor == null) return;
        int objetivo = Math.max(stock.getStockMinimo() * 2, stock.getStockMinimo() + 1);
        int cantidad = Math.max(objetivo - stock.getStockActual(), stock.getStockMinimo());
        OrdenCompra orden = new OrdenCompra();
        orden.setSucursal(stock.getSucursal());
        orden.setProveedor(proveedor);
        orden.setProducto(stock.getProducto());
        orden.setCantidad(cantidad);
        orden.setFechaCreacion(new Date());
        orden.setEstado("PENDIENTE");
        orden.setAutomatica(true);
        ordenCompraRepository.save(orden);
    }

    private int validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        return cantidad;
    }
    private int validarStockMinimo(Integer minimo) {
        if (minimo == null || minimo < 0) throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        return minimo;
    }
    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("Debe indicar tipo de movimiento");
        String valor = tipo.trim().toUpperCase(Locale.ROOT);
        if (!ENTRADA.equals(valor) && !SALIDA.equals(valor)) throw new IllegalArgumentException("Tipo debe ser ENTRADA o SALIDA");
        return valor;
    }
}
