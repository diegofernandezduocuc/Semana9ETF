package com.minimarket.service;

import com.minimarket.dto.DetallePedidoRequest;
import com.minimarket.dto.PedidoRequest;
import com.minimarket.entity.DetallePedido;
import com.minimarket.entity.Pedido;
import com.minimarket.entity.Producto;
import com.minimarket.entity.StockSucursal;
import com.minimarket.entity.Sucursal;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.DetallePedidoRepository;
import com.minimarket.repository.PedidoRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.SucursalRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detalleRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;
    private final StockSucursalService stockSucursalService;
    private final PromocionService promocionService;

    public PedidoService(PedidoRepository pedidoRepository, DetallePedidoRepository detalleRepository,
                         UsuarioRepository usuarioRepository, SucursalRepository sucursalRepository,
                         ProductoRepository productoRepository, StockSucursalService stockSucursalService,
                         PromocionService promocionService) {
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.usuarioRepository = usuarioRepository;
        this.sucursalRepository = sucursalRepository;
        this.productoRepository = productoRepository;
        this.stockSucursalService = stockSucursalService;
        this.promocionService = promocionService;
    }

    public List<Pedido> findAll() {
        Usuario usuario = usuarioAutenticado();
        return esAdmin() ? pedidoRepository.findAll() : pedidoRepository.findByUsuarioId(usuario.getId());
    }

    public Pedido findById(Long id) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (!esAdmin() && !pedido.getUsuario().getUsername().equals(usuarioAutenticado().getUsername())) {
            throw new AccessDeniedException("El pedido pertenece a otro usuario");
        }
        return pedido;
    }

    public List<DetallePedido> findDetalles(Long pedidoId) {
        findById(pedidoId);
        return detalleRepository.findByPedidoId(pedidoId);
    }

    @Transactional
    public Pedido registrar(PedidoRequest request) {
        validarRequest(request);
        Usuario usuario = usuarioAutenticado();
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .filter(s -> Boolean.TRUE.equals(s.getActiva()))
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal activa no encontrada"));
        String tipoEntrega = normalizarTipoEntrega(request.getTipoEntrega(), request.getDireccionDespacho());
        Map<Long, Integer> cantidades = agrupar(request.getDetalles());
        Map<Long, Producto> productos = new LinkedHashMap<>();
        Map<Long, StockSucursal> stocks = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            StockSucursal stock = stockSucursalService.obtenerParaActualizacion(sucursal.getId(), producto.getId());
            stockSucursalService.validarStock(stock, entry.getValue());
            productos.put(entry.getKey(), producto);
            stocks.put(entry.getKey(), stock);
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setSucursal(sucursal);
        pedido.setFecha(new Date());
        pedido.setTipoEntrega(tipoEntrega);
        pedido.setDireccionDespacho("DESPACHO_DOMICILIO".equals(tipoEntrega) ? request.getDireccionDespacho().trim() : null);
        pedido.setEstado("CONFIRMADO");
        pedido.setTotal(0.0);
        Pedido guardado = pedidoRepository.save(pedido);

        List<DetallePedido> detalles = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            int cantidad = entry.getValue();
            double precioUnitario = promocionService.precioVigente(producto);
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(guardado);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(precioUnitario * cantidad);
            detalles.add(detalleRepository.save(detalle));
            total += detalle.getSubtotal();
            stockSucursalService.descontar(stocks.get(entry.getKey()), cantidad);
        }
        guardado.setDetalles(detalles);
        guardado.setTotal(total);
        return pedidoRepository.save(guardado);
    }

    public Pedido actualizarEstado(Long id, String estado) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (estado == null || estado.isBlank()) throw new IllegalArgumentException("Debe indicar estado");
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CONFIRMADO", "PREPARANDO", "LISTO_RETIRO", "EN_DESPACHO", "ENTREGADO", "CANCELADO").contains(normalizado)) {
            throw new IllegalArgumentException("Estado de pedido inválido");
        }
        pedido.setEstado(normalizado);
        return pedidoRepository.save(pedido);
    }

    private void validarRequest(PedidoRequest request) {
        if (request == null || request.getSucursalId() == null || request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Sucursal y detalles son obligatorios");
        }
    }

    private Map<Long, Integer> agrupar(List<DetallePedidoRequest> detalles) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetallePedidoRequest detalle : detalles) {
            if (detalle == null || detalle.getProductoId() == null || detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("Cada detalle debe indicar producto y cantidad positiva");
            }
            cantidades.merge(detalle.getProductoId(), detalle.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    private String normalizarTipoEntrega(String tipo, String direccion) {
        if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("Debe indicar tipo de entrega");
        String normalizado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!List.of("RETIRO_TIENDA", "DESPACHO_DOMICILIO").contains(normalizado)) {
            throw new IllegalArgumentException("Tipo de entrega inválido");
        }
        if ("DESPACHO_DOMICILIO".equals(normalizado) && (direccion == null || direccion.isBlank())) {
            throw new IllegalArgumentException("Debe indicar dirección para despacho");
        }
        return normalizado;
    }

    private Usuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) throw new AccessDeniedException("Usuario no autenticado");
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado"));
    }

    private boolean esAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
