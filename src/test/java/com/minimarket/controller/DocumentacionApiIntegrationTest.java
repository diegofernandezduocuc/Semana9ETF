package com.minimarket.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.Carrito;
import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.CarritoRepository;
import com.minimarket.repository.DetalleVentaRepository;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentacionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Test
    void generaContratoOpenApiConJwtYEndpointsPrincipales() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("MiniMarket Plus API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/register']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/productos']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/carrito']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/inventario']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/ventas']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/ventas/{ventaId}/detalles']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/detalle-ventas']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/usuarios']['get']").exists());
    }

    @Test
    void contratoOpenApiMantieneBearerGlobalYMarcaAuthPublica() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode api = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode globalSecurity = api.path("security");
        JsonNode loginSecurity = api.path("paths").path("/api/auth/login").path("post").path("security");
        JsonNode registerSecurity = api.path("paths").path("/api/auth/register").path("post").path("security");

        Assertions.assertTrue(globalSecurity.isArray());
        Assertions.assertTrue(globalSecurity.size() > 0);
        Assertions.assertTrue(globalSecurity.get(0).has("bearerAuth"));
        Assertions.assertTrue(loginSecurity.isArray());
        Assertions.assertEquals(0, loginSecurity.size());
        Assertions.assertTrue(registerSecurity.isArray());
        Assertions.assertEquals(0, registerSecurity.size());
    }

    @Test
    void contratoOpenApiDocumentaInventarioYVentasConCodigosEsperados() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode api = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode inventarioOperacion = api.path("paths").path("/api/inventario").path("post");
        JsonNode inventarioPost = inventarioOperacion.path("responses");
        JsonNode ventasPost = api.path("paths").path("/api/ventas").path("post");
        JsonNode inventarioRequest = inventarioOperacion.path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema");
        JsonNode ventaRequest = ventasPost.path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema");

        Assertions.assertEquals("#/components/schemas/InventarioRequest", inventarioRequest.path("$ref").asText());
        Assertions.assertEquals("#/components/schemas/VentaRequest", ventaRequest.path("$ref").asText());
        for (String codigo : List.of("201", "400", "401", "403", "404", "409")) {
            Assertions.assertTrue(inventarioPost.has(codigo));
            Assertions.assertTrue(ventasPost.path("responses").has(codigo));
        }
    }

    @Test
    void contratoOpenApiNoDocumentaEscriturasRetiradasDeDetalleEInventario() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode paths = objectMapper.readTree(result.getResponse().getContentAsString()).path("paths");
        Assertions.assertFalse(paths.path("/api/detalle-ventas").has("post"));
        Assertions.assertFalse(paths.path("/api/detalle-ventas/{id}").has("put"));
        Assertions.assertFalse(paths.path("/api/detalle-ventas/{id}").has("delete"));
        Assertions.assertFalse(paths.path("/api/inventario/{id}").has("put"));
        Assertions.assertFalse(paths.path("/api/inventario/{id}").has("delete"));
        Assertions.assertTrue(paths.path("/api/ventas/{ventaId}/detalles").has("get"));
    }

    @Test
    void loginGeneraTokenJwtConRoles() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void registroPublicoCreaClienteConPasswordCifrada() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "registroCliente",
                                  "correo": "registro.cliente@minimarket.local",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("registroCliente"))
                .andExpect(jsonPath("$.correo").value("registro.cliente@minimarket.local"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Usuario usuario = usuarioRepository.findByUsername("registroCliente").orElseThrow();
        Assertions.assertTrue(usuario.getPassword().startsWith("$2"));
        Assertions.assertTrue(usuario.getRoles().stream()
                .anyMatch(rol -> rol.getNombre().equals("ROLE_CLIENTE")));
    }

    @Test
    void registroPublicoAceptaDatosValidosConReglasDeUsuario() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "validacionCliente",
                                  "correo": "validacion.cliente@minimarket.local",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("validacionCliente"))
                .andExpect(jsonPath("$.correo").value("validacion.cliente@minimarket.local"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENTE"));
    }

    @Test
    void registroPublicoRechazaRolAdministrativo() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "intentoAdmin",
                                  "correo": "intento.admin@minimarket.local",
                                  "password": "cliente123",
                                  "roles": ["ROLE_ADMIN"]
                                }
                                """))
                .andExpect(status().isBadRequest());

        Assertions.assertTrue(usuarioRepository.findByUsername("intentoAdmin").isEmpty());
    }

    @Test
    void registroPublicoRechazaUsuarioOCorreoDuplicado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "duplicadoCliente",
                                  "correo": "duplicado@minimarket.local",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "duplicadoCliente",
                                  "correo": "otro.duplicado@minimarket.local",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "otroDuplicado",
                                  "correo": "duplicado@minimarket.local",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void registroPublicoRechazaCamposInvalidos() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "sinPassword",
                                  "correo": "sin.password@minimarket.local"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "sinCorreo",
                                  "correo": " ",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroPublicoRechazaCorreoInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "correoInvalido",
                                  "correo": "correo-invalido",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroPublicoRechazaPasswordCorta() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "passwordCorta",
                                  "correo": "password.corta@minimarket.local",
                                  "password": "1234567"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroPublicoRechazaUsernameCorto() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "correo": "username.corto@minimarket.local",
                                  "password": "cliente123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaCredencialesInvalidas() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recursoProtegidoRechazaSolicitudSinToken() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recursoProtegidoRechazaTokenAlterado() throws Exception {
        String token = token("cliente", "cliente123");

        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer " + alterarFirma(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recursoProtegidoRechazaTokenExpirado() throws Exception {
        String token = tokenExpirado("cliente", List.of("ROLE_CLIENTE"));

        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productoIncluyeEnlacesHateoasConJwt() throws Exception {
        Producto producto = productoRepository.findAll().get(0);

        mockMvc.perform(get("/api/productos/{id}", producto.getId())
                        .header("Authorization", bearer("admin", "admin123"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.productos.href").exists())
                .andExpect(jsonPath("$._links.categoria.href").exists());
    }

    @Test
    void recursosPrincipalesIncluyenNavegacionHateoas() throws Exception {
        Carrito carrito = carritoRepository.findAll().get(0);
        Inventario inventario = inventarioRepository.findAll().get(0);
        Usuario usuario = usuarioRepository.findByUsername("cliente").orElseThrow();
        String admin = bearer("admin", "admin123");

        mockMvc.perform(get("/api/carrito/{id}", carrito.getId())
                        .header("Authorization", admin)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.usuario.href").exists())
                .andExpect(jsonPath("$._links.producto.href").exists());

        mockMvc.perform(get("/api/inventario/{id}", inventario.getId())
                        .header("Authorization", admin)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.producto.href").exists());

        mockMvc.perform(get("/api/usuarios/{id}", usuario.getId())
                        .header("Authorization", admin)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.usuarios.href").exists())
                .andExpect(jsonPath("$._links.carrito.href").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void inventarioEntradaAumentaStockYRegistraMovimiento() throws Exception {
        Producto producto = crearProductoParaPrueba("Entrada stock", 10, 100.0);
        long movimientosAntes = inventarioRepository.count();

        MvcResult result = mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 5,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(5))
                .andExpect(jsonPath("$.tipoMovimiento").value("ENTRADA"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.producto.href").exists())
                .andReturn();

        Long movimientoId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        Assertions.assertEquals(15, actualizado.getStock());
        Assertions.assertEquals(movimientosAntes + 1, inventarioRepository.count());
        Assertions.assertTrue(inventarioRepository.findById(movimientoId).isPresent());
    }

    @Test
    void inventarioSalidaDisminuyeStockYRegistraMovimiento() throws Exception {
        Producto producto = crearProductoParaPrueba("Salida stock", 10, 100.0);

        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 4,
                                  "tipoMovimiento": "SALIDA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA"));

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        Assertions.assertEquals(6, actualizado.getStock());
        Assertions.assertEquals(1, inventarioRepository.findByProductoId(producto.getId()).size());
    }

    @Test
    void inventarioRechazaCantidadCeroONegativaSinModificarStock() throws Exception {
        Producto producto = crearProductoParaPrueba("Cantidad invalida", 10, 100.0);
        long movimientosAntes = inventarioRepository.count();

        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 0,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": -1,
                                  "tipoMovimiento": "SALIDA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isBadRequest());

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        Assertions.assertEquals(10, actualizado.getStock());
        Assertions.assertEquals(movimientosAntes, inventarioRepository.count());
    }

    @Test
    void inventarioRechazaProductoInexistente() throws Exception {
        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": 999999,
                                  "cantidad": 1,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void inventarioRechazaStockInsuficienteSinCambiosParciales() throws Exception {
        Producto producto = crearProductoParaPrueba("Stock insuficiente", 2, 100.0);
        long movimientosAntes = inventarioRepository.count();

        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 5,
                                  "tipoMovimiento": "SALIDA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isConflict());

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        Assertions.assertEquals(2, actualizado.getStock());
        Assertions.assertEquals(movimientosAntes, inventarioRepository.count());
    }

    @Test
    void inventarioMovimientosSonInmutablesYPutDeleteRespondenMethodNotAllowed() throws Exception {
        Inventario movimiento = inventarioRepository.findAll().get(0);
        Producto producto = productoRepository.findAll().get(0);
        long movimientosAntes = inventarioRepository.count();

        mockMvc.perform(put("/api/inventario/{id}", movimiento.getId())
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 1,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/inventario/{id}", movimiento.getId())
                        .header("Authorization", bearer("admin", "admin123")))
                .andExpect(status().isMethodNotAllowed());

        Assertions.assertEquals(movimientosAntes, inventarioRepository.count());
    }

    @Test
    void ventaValidaDeUnProductoDescuentaStockYRegistraDetalleMovimiento() throws Exception {
        Producto producto = crearProductoParaPrueba("Venta unitaria", 10, 250.0);

        MvcResult result = mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 3,
                                      "precio": 1.0
                                    }
                                  ]
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links['detalle-ventas'].href").exists())
                .andExpect(jsonPath("$..password").doesNotExist())
                .andReturn();

        Long ventaId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        String detallesHref = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("_links")
                .path("detalle-ventas")
                .path("href")
                .asText();
        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        Venta venta = ventaRepository.findById(ventaId).orElseThrow();
        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(ventaId);
        List<Inventario> movimientos = inventarioRepository.findByProductoId(producto.getId());
        Assertions.assertTrue(detallesHref.endsWith("/api/ventas/" + ventaId + "/detalles"));
        Assertions.assertEquals("cajero", venta.getUsuario().getUsername());
        Assertions.assertEquals(7, actualizado.getStock());
        Assertions.assertEquals(1, detalles.size());
        Assertions.assertEquals(750.0, detalles.get(0).getPrecio());
        Assertions.assertEquals(1, movimientos.size());
        Assertions.assertEquals("SALIDA", movimientos.get(0).getTipoMovimiento());
    }

    @Test
    void ventaValidaDeVariosProductosDescuentaCadaStock() throws Exception {
        Producto arroz = crearProductoParaPrueba("Venta multiple A", 10, 100.0);
        Producto leche = crearProductoParaPrueba("Venta multiple B", 8, 200.0);

        MvcResult result = mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 2 },
                                    { "productoId": %d, "cantidad": 3 }
                                  ]
                                }
                                """.formatted(arroz.getId(), leche.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        Long ventaId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        Assertions.assertEquals(8, productoRepository.findById(arroz.getId()).orElseThrow().getStock());
        Assertions.assertEquals(5, productoRepository.findById(leche.getId()).orElseThrow().getStock());
        Assertions.assertEquals(2, detalleVentaRepository.findByVentaId(ventaId).size());
        Assertions.assertEquals(1, inventarioRepository.findByProductoId(arroz.getId()).size());
        Assertions.assertEquals(1, inventarioRepository.findByProductoId(leche.getId()).size());
    }

    @Test
    void detallesDeVentaSonSoloLecturaYEscriturasRespondenMethodNotAllowed() throws Exception {
        Producto producto = crearProductoParaPrueba("Detalle solo lectura", 10, 100.0);

        MvcResult ventaResult = mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 1 }
                                  ]
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        Long ventaId = objectMapper.readTree(ventaResult.getResponse().getContentAsString()).get("id").asLong();
        Long detalleId = detalleVentaRepository.findByVentaId(ventaId).get(0).getId();
        long detallesAntes = detalleVentaRepository.count();

        mockMvc.perform(post("/api/detalle-ventas")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "venta": { "id": %d },
                                  "producto": { "id": %d },
                                  "cantidad": 1,
                                  "precio": 1.0
                                }
                                """.formatted(ventaId, producto.getId())))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/detalle-ventas/{id}", detalleId)
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cantidad": 99
                                }
                                """))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/detalle-ventas/{id}", detalleId)
                        .header("Authorization", bearer("admin", "admin123")))
                .andExpect(status().isMethodNotAllowed());

        Assertions.assertEquals(detallesAntes, detalleVentaRepository.count());
    }

    @Test
    void listarDetallesDeVentaDevuelveSoloLosDeEsaVenta() throws Exception {
        Producto primero = crearProductoParaPrueba("Detalle venta A", 10, 100.0);
        Producto segundo = crearProductoParaPrueba("Detalle venta B", 10, 200.0);

        MvcResult primeraVenta = mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 2 }
                                  ]
                                }
                                """.formatted(primero.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult segundaVenta = mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 1 }
                                  ]
                                }
                                """.formatted(segundo.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        Long primeraVentaId = objectMapper.readTree(primeraVenta.getResponse().getContentAsString()).get("id").asLong();
        Long segundaVentaId = objectMapper.readTree(segundaVenta.getResponse().getContentAsString()).get("id").asLong();
        Long detallePrimeraVenta = detalleVentaRepository.findByVentaId(primeraVentaId).get(0).getId();
        Long detalleSegundaVenta = detalleVentaRepository.findByVentaId(segundaVentaId).get(0).getId();

        mockMvc.perform(get("/api/ventas/{ventaId}/detalles", primeraVentaId)
                        .header("Authorization", bearer("admin", "admin123"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.venta.href").exists())
                .andExpect(jsonPath("$._embedded.detalleVentaList.length()").value(1))
                .andExpect(jsonPath("$._embedded.detalleVentaList[0].id").value(detallePrimeraVenta))
                .andExpect(jsonPath("$._embedded.detalleVentaList[0].id").value(org.hamcrest.Matchers.not(detalleSegundaVenta.intValue())));
    }

    @Test
    void listarDetallesDeVentaInexistenteRespondeNotFound() throws Exception {
        mockMvc.perform(get("/api/ventas/{ventaId}/detalles", 999999L)
                        .header("Authorization", bearer("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void ventaRechazaSolicitudSinDetalles() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ventaRevierteTodoCuandoFallaUnDetalle() throws Exception {
        Producto suficiente = crearProductoParaPrueba("Rollback venta A", 10, 100.0);
        Producto insuficiente = crearProductoParaPrueba("Rollback venta B", 1, 200.0);
        long ventasAntes = ventaRepository.count();
        long detallesAntes = detalleVentaRepository.count();
        long movimientosAntes = inventarioRepository.count();

        mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 2 },
                                    { "productoId": %d, "cantidad": 5 }
                                  ]
                                }
                                """.formatted(suficiente.getId(), insuficiente.getId())))
                .andExpect(status().isConflict());

        Assertions.assertEquals(10, productoRepository.findById(suficiente.getId()).orElseThrow().getStock());
        Assertions.assertEquals(1, productoRepository.findById(insuficiente.getId()).orElseThrow().getStock());
        Assertions.assertEquals(ventasAntes, ventaRepository.count());
        Assertions.assertEquals(detallesAntes, detalleVentaRepository.count());
        Assertions.assertEquals(movimientosAntes, inventarioRepository.count());
    }

    @Test
    void ventaAgrupaProductoRepetidoAntesDeValidarStock() throws Exception {
        Producto producto = crearProductoParaPrueba("Venta repetida", 4, 100.0);
        long ventasAntes = ventaRepository.count();
        long detallesAntes = detalleVentaRepository.count();
        long movimientosAntes = inventarioRepository.count();

        mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 3 },
                                    { "productoId": %d, "cantidad": 2 }
                                  ]
                                }
                                """.formatted(producto.getId(), producto.getId())))
                .andExpect(status().isConflict());

        Assertions.assertEquals(4, productoRepository.findById(producto.getId()).orElseThrow().getStock());
        Assertions.assertEquals(ventasAntes, ventaRepository.count());
        Assertions.assertEquals(detallesAntes, detalleVentaRepository.count());
        Assertions.assertEquals(movimientosAntes, inventarioRepository.count());
        Assertions.assertEquals(0, inventarioRepository.findByProductoId(producto.getId()).size());
    }

    @Test
    void ventaUsaPrecioDelProductoYNoElPrecioEnviado() throws Exception {
        Producto producto = crearProductoParaPrueba("Precio producto", 10, 1234.0);

        MvcResult result = mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuario": { "id": 999999 },
                                  "total": 1.0,
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 2,
                                      "precio": 1.0,
                                      "stockFinal": 999,
                                      "ventaId": 999999,
                                      "producto": { "id": 999999 }
                                    }
                                  ]
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        Long ventaId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        DetalleVenta detalle = detalleVentaRepository.findByVentaId(ventaId).get(0);
        Assertions.assertEquals(2468.0, detalle.getPrecio());
    }

    @Test
    void permisosDeInventarioYVentasRespetanRoles() throws Exception {
        Producto productoInventario = crearProductoParaPrueba("Permiso inventario", 5, 100.0);
        Producto productoVenta = crearProductoParaPrueba("Permiso venta", 5, 100.0);

        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 1,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """.formatted(productoInventario.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 1,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """.formatted(productoInventario.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 1 }
                                  ]
                                }
                                """.formatted(productoVenta.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ventas")
                        .header("Authorization", bearer("cajero", "cajero123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 1 }
                                  ]
                                }
                                """.formatted(productoVenta.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void inventarioYVentasRechazanTokenAusenteOAlterado() throws Exception {
        Producto producto = crearProductoParaPrueba("Token inventario venta", 5, 100.0);
        String tokenAlterado = alterarFirma(token("admin", "admin123"));

        mockMvc.perform(post("/api/inventario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 1,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 1 }
                                  ]
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/inventario")
                        .header("Authorization", "Bearer " + tokenAlterado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": %d,
                                  "cantidad": 1,
                                  "tipoMovimiento": "ENTRADA"
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteCreaCarritoPropioCorrectamente() throws Exception {
        Producto producto = productoRepository.findAll().get(0);
        Usuario cliente = usuarioRepository.findByUsername("cliente").orElseThrow();

        MvcResult result = mockMvc.perform(post("/api/carrito")
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .content("""
                                {
                                  "producto": { "id": %d },
                                  "cantidad": 1
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.id").value(cliente.getId()))
                .andExpect(jsonPath("$.usuario.username").value("cliente"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        Carrito guardado = carritoRepository.findById(id).orElseThrow();
        Assertions.assertEquals(cliente.getId(), guardado.getUsuario().getId());
    }

    @Test
    void clienteNoPuedeSeleccionarOtroUsuarioEnBody() throws Exception {
        Producto producto = productoRepository.findAll().get(0);
        Usuario cliente = usuarioRepository.findByUsername("cliente").orElseThrow();
        Usuario admin = usuarioRepository.findByUsername("admin").orElseThrow();

        MvcResult result = mockMvc.perform(post("/api/carrito")
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .content("""
                                {
                                  "usuarioId": %d,
                                  "username": "admin",
                                  "usuario": { "id": %d, "username": "admin" },
                                  "producto": { "id": %d },
                                  "cantidad": 2
                                }
                                """.formatted(admin.getId(), admin.getId(), producto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.id").value(cliente.getId()))
                .andExpect(jsonPath("$.usuario.username").value("cliente"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist())
                .andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        Carrito guardado = carritoRepository.findById(id).orElseThrow();
        Assertions.assertEquals(cliente.getId(), guardado.getUsuario().getId());
        Assertions.assertNotEquals(admin.getId(), guardado.getUsuario().getId());
    }

    @Test
    void clienteConsultaSuPropioCarrito() throws Exception {
        Carrito carrito = crearCarrito("cliente", 1);

        mockMvc.perform(get("/api/carrito/{id}", carrito.getId())
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(carrito.getId()))
                .andExpect(jsonPath("$.usuario.username").value("cliente"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    void clienteNoPuedeConsultarCarritoAjeno() throws Exception {
        Carrito ajeno = crearCarrito("admin", 1);

        mockMvc.perform(get("/api/carrito/{id}", ajeno.getId())
                        .header("Authorization", bearer("cliente", "cliente123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeActualizarCarritoAjeno() throws Exception {
        Carrito ajeno = crearCarrito("admin", 1);
        Producto producto = productoRepository.findAll().get(0);

        mockMvc.perform(put("/api/carrito/{id}", ajeno.getId())
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "producto": { "id": %d },
                                  "cantidad": 3
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeEliminarCarritoAjeno() throws Exception {
        Carrito ajeno = crearCarrito("admin", 1);

        mockMvc.perform(delete("/api/carrito/{id}", ajeno.getId())
                        .header("Authorization", bearer("cliente", "cliente123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void carritoInexistenteRespondeNotFound() throws Exception {
        mockMvc.perform(get("/api/carrito/{id}", 999999L)
                        .header("Authorization", bearer("cliente", "cliente123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminPuedeListarYConsultarTodosLosCarritos() throws Exception {
        Carrito cliente = crearCarrito("cliente", 1);
        Carrito admin = crearCarrito("admin", 1);
        String tokenAdmin = bearer("admin", "admin123");

        mockMvc.perform(get("/api/carrito")
                        .header("Authorization", tokenAdmin)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..password").doesNotExist());

        mockMvc.perform(get("/api/carrito/{id}", cliente.getId())
                        .header("Authorization", tokenAdmin)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/carrito/{id}", admin.getId())
                        .header("Authorization", tokenAdmin)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void adminPuedeModificarYEliminarCarrito() throws Exception {
        Carrito carrito = crearCarrito("cliente", 1);
        Producto producto = productoRepository.findAll().get(0);
        String tokenAdmin = bearer("admin", "admin123");

        mockMvc.perform(put("/api/carrito/{id}", carrito.getId())
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .content("""
                                {
                                  "producto": { "id": %d },
                                  "cantidad": 5
                                }
                                """.formatted(producto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(5))
                .andExpect(jsonPath("$.usuario.username").value("cliente"));

        mockMvc.perform(delete("/api/carrito/{id}", carrito.getId())
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/carrito/{id}", carrito.getId())
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void carritoRechazaSolicitudSinToken() throws Exception {
        mockMvc.perform(get("/api/carrito"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void carritoRechazaTokenAlterado() throws Exception {
        String token = token("cliente", "cliente123");

        mockMvc.perform(get("/api/carrito")
                        .header("Authorization", "Bearer " + alterarFirma(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cajeroNoObtienePermisosNuevosSobreCarrito() throws Exception {
        mockMvc.perform(get("/api/carrito")
                        .header("Authorization", bearer("cajero", "cajero123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientePuedeLeerProductosPeroNoModificarlos() throws Exception {
        String cliente = bearer("cliente", "cliente123");

        mockMvc.perform(get("/api/productos")
                        .header("Authorization", cliente)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", cliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeAccederAOperacionAdministrativa() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", bearer("admin", "admin123"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    void adminNoPuedeCrearUsuarioConDatosInvalidos() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "xy",
                                  "correo": "admin-invalido",
                                  "password": "1234567",
                                  "roles": [
                                    { "nombre": "ROLE_CLIENTE" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cajeroPuedeConsultarVentasPeroNoAdministrarProductos() throws Exception {
        String cajero = bearer("cajero", "cajero123");

        mockMvc.perform(get("/api/ventas")
                        .header("Authorization", cajero))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerUiEsPublico() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void h2ConsoleNoEstaDisponiblePublicamente() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/h2-console/")
                        .header("Authorization", bearer("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    private String bearer(String username, String password) throws Exception {
        return "Bearer " + token(username, password);
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    private String tokenExpirado(String username, List<String> roles) {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60))
                .subject(username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String alterarFirma(String token) {
        int inicioFirma = token.lastIndexOf('.') + 1;
        char actual = token.charAt(inicioFirma);
        char reemplazo = actual == 'a' ? 'b' : 'a';
        return token.substring(0, inicioFirma) + reemplazo + token.substring(inicioFirma + 1);
    }

    private Carrito crearCarrito(String username, int cantidad) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        Producto producto = productoRepository.findAll().get(0);
        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setProducto(producto);
        carrito.setCantidad(cantidad);
        return carritoRepository.save(carrito);
    }

    private Producto crearProductoParaPrueba(String nombre, int stock, double precio) {
        Producto base = productoRepository.findAll().get(0);
        Producto producto = new Producto();
        producto.setNombre(nombre + " " + System.nanoTime());
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setCategoria(base.getCategoria());
        return productoRepository.save(producto);
    }
}
