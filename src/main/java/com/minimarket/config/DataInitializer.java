package com.minimarket.config;

import com.minimarket.entity.Carrito;
import com.minimarket.entity.Categoria;
import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Promocion;
import com.minimarket.entity.Proveedor;
import com.minimarket.entity.Rol;
import com.minimarket.entity.StockSucursal;
import com.minimarket.entity.Sucursal;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.CarritoRepository;
import com.minimarket.repository.CategoriaRepository;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.PromocionRepository;
import com.minimarket.repository.ProveedorRepository;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.StockSucursalRepository;
import com.minimarket.repository.SucursalRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner cargarDatosIniciales(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository,
            InventarioRepository inventarioRepository,
            CarritoRepository carritoRepository,
            SucursalRepository sucursalRepository,
            ProveedorRepository proveedorRepository,
            StockSucursalRepository stockSucursalRepository,
            PromocionRepository promocionRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            Rol adminRol = rolRepository.findByNombre("ROLE_ADMIN")
                    .orElseGet(() -> rolRepository.save(new Rol("ROLE_ADMIN")));
            Rol clienteRol = rolRepository.findByNombre("ROLE_CLIENTE")
                    .orElseGet(() -> rolRepository.save(new Rol("ROLE_CLIENTE")));
            Rol cajeroRol = rolRepository.findByNombre("ROLE_CAJERO")
                    .orElseGet(() -> rolRepository.save(new Rol("ROLE_CAJERO")));

            usuarioRepository.findByUsername("admin")
                    .orElseGet(() -> crearUsuario(
                            usuarioRepository, passwordEncoder, "admin", "admin@minimarket.local",
                            "admin123", Set.of(adminRol)));
            Usuario cliente = usuarioRepository.findByUsername("cliente")
                    .orElseGet(() -> crearUsuario(
                            usuarioRepository, passwordEncoder, "cliente", "cliente@minimarket.local",
                            "cliente123", Set.of(clienteRol)));
            usuarioRepository.findByUsername("cajero")
                    .orElseGet(() -> crearUsuario(
                            usuarioRepository, passwordEncoder, "cajero", "cajero@minimarket.local",
                            "cajero123", Set.of(cajeroRol)));

            if (categoriaRepository.count() == 0) {
                Categoria abarrotes = crearCategoria(categoriaRepository, "Abarrotes");
                Categoria lacteos = crearCategoria(categoriaRepository, "Lácteos");

                Producto arroz = crearProducto(productoRepository, "Arroz 1 kg", 1890.0, 40, abarrotes);
                Producto leche = crearProducto(productoRepository, "Leche entera 1 L", 1290.0, 25, lacteos);

                crearMovimiento(inventarioRepository, arroz, 40, "ENTRADA");
                crearMovimiento(inventarioRepository, leche, 25, "ENTRADA");

                Carrito carrito = new Carrito();
                carrito.setUsuario(cliente);
                carrito.setProducto(arroz);
                carrito.setCantidad(2);
                carritoRepository.save(carrito);
            }

            Proveedor proveedor = proveedorRepository.findFirstByActivoTrueOrderByIdAsc()
                    .orElseGet(() -> crearProveedor(proveedorRepository));
            Sucursal centro = sucursalRepository.findByNombre("Sucursal Centro")
                    .orElseGet(() -> crearSucursal(sucursalRepository, "Sucursal Centro", "Av. Principal 100, Santiago"));
            Sucursal norte = sucursalRepository.findByNombre("Sucursal Norte")
                    .orElseGet(() -> crearSucursal(sucursalRepository, "Sucursal Norte", "Av. Norte 250, Santiago"));

            List<Producto> productos = productoRepository.findAll();
            for (Producto producto : productos) {
                crearStockSucursalSiFalta(stockSucursalRepository, centro, producto,
                        Math.max(10, producto.getStock() == null ? 10 : producto.getStock() / 2), 5);
                crearStockSucursalSiFalta(stockSucursalRepository, norte, producto,
                        Math.max(8, producto.getStock() == null ? 8 : producto.getStock() / 3), 4);
            }

            if (promocionRepository.count() == 0 && !productos.isEmpty()) {
                Promocion promocion = new Promocion();
                promocion.setNombre("Oferta semanal");
                promocion.setProducto(productos.get(0));
                promocion.setPorcentajeDescuento(10.0);
                promocion.setFechaInicio(LocalDate.now().minusDays(1));
                promocion.setFechaFin(LocalDate.now().plusDays(7));
                promocion.setActiva(true);
                promocionRepository.save(promocion);
            }

            if (proveedor.getId() == null) {
                throw new IllegalStateException("No fue posible inicializar el proveedor de demostración");
            }
        };
    }

    private Categoria crearCategoria(CategoriaRepository repository, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return repository.save(categoria);
    }

    private Usuario crearUsuario(
            UsuarioRepository repository,
            PasswordEncoder encoder,
            String username,
            String correo,
            String password,
            Set<Rol> roles) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setCorreo(correo);
        usuario.setPassword(encoder.encode(password));
        usuario.setRoles(roles);
        return repository.save(usuario);
    }

    private Producto crearProducto(
            ProductoRepository repository,
            String nombre,
            Double precio,
            Integer stock,
            Categoria categoria) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setCategoria(categoria);
        return repository.save(producto);
    }

    private void crearMovimiento(
            InventarioRepository repository,
            Producto producto,
            Integer cantidad,
            String tipo) {
        Inventario movimiento = new Inventario();
        movimiento.setProducto(producto);
        movimiento.setCantidad(cantidad);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setFechaMovimiento(new Date());
        repository.save(movimiento);
    }

    private Proveedor crearProveedor(ProveedorRepository repository) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Distribuidora Central");
        proveedor.setCorreo("compras@distribuidora.local");
        proveedor.setTelefono("+56 2 2222 2222");
        proveedor.setActivo(true);
        return repository.save(proveedor);
    }

    private Sucursal crearSucursal(SucursalRepository repository, String nombre, String direccion) {
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(nombre);
        sucursal.setDireccion(direccion);
        sucursal.setActiva(true);
        return repository.save(sucursal);
    }

    private void crearStockSucursalSiFalta(
            StockSucursalRepository repository,
            Sucursal sucursal,
            Producto producto,
            int stock,
            int minimo) {
        if (repository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).isPresent()) {
            return;
        }
        StockSucursal item = new StockSucursal();
        item.setSucursal(sucursal);
        item.setProducto(producto);
        item.setStockActual(stock);
        item.setStockMinimo(minimo);
        repository.save(item);
    }
}
