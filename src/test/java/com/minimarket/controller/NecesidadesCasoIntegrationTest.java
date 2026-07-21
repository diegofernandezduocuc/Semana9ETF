package com.minimarket.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.OrdenCompra;
import com.minimarket.entity.Producto;
import com.minimarket.entity.StockSucursal;
import com.minimarket.entity.Sucursal;
import com.minimarket.repository.OrdenCompraRepository;
import com.minimarket.repository.PedidoRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.StockSucursalRepository;
import com.minimarket.repository.SucursalRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NecesidadesCasoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private StockSucursalRepository stockSucursalRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private PedidoRepository pedidoRepository;

    @Test
    void openApiDocumentaNecesidadesAmpliadasDelCaso() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/sucursales']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/stock-sucursales/{sucursalId}/{productoId}/disponibilidad']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/proveedores']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/ordenes-compra']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/promociones/activas']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/pedidos']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/reportes/rotacion-productos']['get']").exists());
    }

    @Test
    void clienteConsultaDisponibilidadPorSucursal() throws Exception {
        Sucursal sucursal = sucursalRepository.findAll().get(0);
        Producto producto = productoRepository.findAll().get(0);

        mockMvc.perform(get("/api/stock-sucursales/{sucursalId}/{productoId}/disponibilidad",
                        sucursal.getId(), producto.getId())
                        .header("Authorization", bearer("cliente", "cliente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucursalId").value(sucursal.getId()))
                .andExpect(jsonPath("$.productoId").value(producto.getId()))
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void stockMinimoGeneraUnaOrdenAutomaticaYRecibirlaRepone() throws Exception {
        Sucursal sucursal = sucursalRepository.findAll().get(0);
        Producto producto = productoRepository.findAll().get(0);
        StockSucursal stock = stockSucursalRepository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).orElseThrow();
        int salida = stock.getStockActual() - stock.getStockMinimo();

        mockMvc.perform(post("/api/stock-sucursales")
                        .header("Authorization", bearer("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":%d,"productoId":%d,"cantidad":%d,"tipoMovimiento":"SALIDA","stockMinimo":%d}
                                """.formatted(sucursal.getId(), producto.getId(), salida, stock.getStockMinimo())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockActual").value(stock.getStockMinimo()));

        Assertions.assertEquals(1, ordenCompraRepository.findByEstado("PENDIENTE").size());
        OrdenCompra orden = ordenCompraRepository.findByEstado("PENDIENTE").get(0);

        mockMvc.perform(post("/api/ordenes-compra/{id}/recibir", orden.getId())
                        .header("Authorization", bearer("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"));

        StockSucursal repuesto = stockSucursalRepository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).orElseThrow();
        Assertions.assertTrue(repuesto.getStockActual() > repuesto.getStockMinimo());
    }

    @Test
    void ordenAutomaticaNoSeDuplicaMientrasExistaUnaPendiente() throws Exception {
        Sucursal sucursal = sucursalRepository.findAll().get(0);
        Producto producto = productoRepository.findAll().get(0);
        StockSucursal stock = stockSucursalRepository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).orElseThrow();
        int primeraSalida = stock.getStockActual() - stock.getStockMinimo();
        String admin = bearer("admin", "admin123");

        mockMvc.perform(post("/api/stock-sucursales")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":%d,"productoId":%d,"cantidad":%d,"tipoMovimiento":"SALIDA","stockMinimo":%d}
                                """.formatted(sucursal.getId(), producto.getId(), primeraSalida, stock.getStockMinimo())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/stock-sucursales")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":%d,"productoId":%d,"cantidad":1,"tipoMovimiento":"SALIDA"}
                                """.formatted(sucursal.getId(), producto.getId())))
                .andExpect(status().isCreated());

        Assertions.assertEquals(1, ordenCompraRepository.findByEstado("PENDIENTE").size());
    }

    @Test
    void clienteCreaPedidoRetiroConPromocionYDescuentaStockSucursal() throws Exception {
        Sucursal sucursal = sucursalRepository.findAll().get(1);
        Producto producto = productoRepository.findAll().get(0);
        StockSucursal antes = stockSucursalRepository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).orElseThrow();
        int stockAntes = antes.getStockActual();

        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .content("""
                                {"sucursalId":%d,"tipoEntrega":"RETIRO_TIENDA","detalles":[{"productoId":%d,"cantidad":2}]}
                                """.formatted(sucursal.getId(), producto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEntrega").value("RETIRO_TIENDA"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$._links.detalles.href").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(producto.getPrecio() * 2 * 0.9, body.get("total").asDouble(), 0.01);
        StockSucursal despues = stockSucursalRepository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).orElseThrow();
        Assertions.assertEquals(stockAntes - 2, despues.getStockActual());
        Assertions.assertEquals(1, pedidoRepository.count());
    }

    @Test
    void despachoDomicilioExigeDireccionYNoModificaStock() throws Exception {
        Sucursal sucursal = sucursalRepository.findAll().get(0);
        Producto producto = productoRepository.findAll().get(0);
        StockSucursal antes = stockSucursalRepository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).orElseThrow();

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":%d,"tipoEntrega":"DESPACHO_DOMICILIO","detalles":[{"productoId":%d,"cantidad":1}]}
                                """.formatted(sucursal.getId(), producto.getId())))
                .andExpect(status().isBadRequest());

        Assertions.assertEquals(antes.getStockActual(),
                stockSucursalRepository.findById(antes.getId()).orElseThrow().getStockActual());
        Assertions.assertEquals(0, pedidoRepository.count());
    }

    @Test
    void pedidoConProductoInexistenteNoGeneraPersistenciaParcial() throws Exception {
        Sucursal sucursal = sucursalRepository.findAll().get(0);
        Producto producto = productoRepository.findAll().get(0);
        StockSucursal antes = stockSucursalRepository.findBySucursalIdAndProductoId(sucursal.getId(), producto.getId()).orElseThrow();

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", bearer("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":%d,"tipoEntrega":"RETIRO_TIENDA","detalles":[
                                  {"productoId":%d,"cantidad":1},{"productoId":999999,"cantidad":1}
                                ]}
                                """.formatted(sucursal.getId(), producto.getId())))
                .andExpect(status().isNotFound());

        Assertions.assertEquals(0, pedidoRepository.count());
        Assertions.assertEquals(antes.getStockActual(),
                stockSucursalRepository.findById(antes.getId()).orElseThrow().getStockActual());
    }

    @Test
    void reportesAdministrativosRequierenRolAdmin() throws Exception {
        mockMvc.perform(get("/api/reportes/rotacion-productos")
                        .header("Authorization", bearer("cliente", "cliente123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reportes/rotacion-productos")
                        .header("Authorization", bearer("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").exists())
                .andExpect(jsonPath("$[0].totalMovido").exists());
    }

    private String bearer(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
