package com.minimarket.config;

public final class ApiExamples {

    public static final String PRODUCTO_REQUEST = "{\"nombre\":\"Aceite vegetal 1 L\",\"precio\":2490.0,\"stock\":30,\"categoria\":{\"id\":1}}";
    public static final String PRODUCTOS_COLLECTION = "{\"_embedded\":{\"productoList\":[{\"id\":1,\"nombre\":\"Arroz 1 kg\",\"precio\":1890.0,\"stock\":40,\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/productos/1\"}}}]},\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/productos\"}}}";
    public static final String PRODUCTO_RESPONSE = "{\"id\":1,\"nombre\":\"Arroz 1 kg\",\"precio\":1890.0,\"stock\":40,\"categoria\":{\"id\":1,\"nombre\":\"Abarrotes\"},\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/productos/1\"},\"productos\":{\"href\":\"http://localhost:8080/api/productos\"},\"categoria\":{\"href\":\"http://localhost:8080/api/categorias/1\"}}}";

    public static final String CARRITO_REQUEST = "{\"usuario\":{\"id\":2},\"producto\":{\"id\":1},\"cantidad\":2}";
    public static final String CARRITO_COLLECTION = "{\"_embedded\":{\"carritoList\":[{\"id\":1,\"cantidad\":2,\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/carrito/1\"}}}]},\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/carrito\"}}}";
    public static final String CARRITO_RESPONSE = "{\"id\":1,\"usuario\":{\"id\":2,\"username\":\"cliente\",\"roles\":[{\"id\":2,\"nombre\":\"ROLE_CLIENTE\"}]},\"producto\":{\"id\":1,\"nombre\":\"Arroz 1 kg\",\"precio\":1890.0,\"stock\":40,\"categoria\":{\"id\":1,\"nombre\":\"Abarrotes\"}},\"cantidad\":2,\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/carrito/1\"},\"carrito\":{\"href\":\"http://localhost:8080/api/carrito\"},\"usuario\":{\"href\":\"http://localhost:8080/api/usuarios/2\"},\"producto\":{\"href\":\"http://localhost:8080/api/productos/1\"}}}";

    public static final String INVENTARIO_REQUEST = "{\"productoId\":1,\"cantidad\":10,\"tipoMovimiento\":\"ENTRADA\"}";
    public static final String INVENTARIO_COLLECTION = "{\"_embedded\":{\"inventarioList\":[{\"id\":1,\"cantidad\":40,\"tipoMovimiento\":\"ENTRADA\",\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/inventario/1\"}}}]},\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/inventario\"}}}";
    public static final String INVENTARIO_RESPONSE = "{\"id\":1,\"producto\":{\"id\":1,\"nombre\":\"Arroz 1 kg\",\"precio\":1890.0,\"stock\":40,\"categoria\":{\"id\":1,\"nombre\":\"Abarrotes\"}},\"cantidad\":40,\"tipoMovimiento\":\"ENTRADA\",\"fechaMovimiento\":\"2026-07-13T20:00:00.000+00:00\",\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/inventario/1\"},\"inventario\":{\"href\":\"http://localhost:8080/api/inventario\"},\"producto\":{\"href\":\"http://localhost:8080/api/productos/1\"}}}";

    public static final String VENTA_REQUEST = "{\"detalles\":[{\"productoId\":1,\"cantidad\":2}]}";
    public static final String VENTA_RESPONSE = "{\"id\":1,\"usuario\":{\"id\":3,\"username\":\"cajero\",\"roles\":[{\"id\":3,\"nombre\":\"ROLE_CAJERO\"}]},\"fecha\":\"2026-07-13T20:00:00.000+00:00\",\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/ventas/1\"},\"ventas\":{\"href\":\"http://localhost:8080/api/ventas\"},\"detalle-ventas\":{\"href\":\"http://localhost:8080/api/ventas/1/detalles\"},\"inventario\":{\"href\":\"http://localhost:8080/api/inventario\"}}}";
    public static final String DETALLE_VENTA_RESPONSE = "{\"id\":1,\"producto\":{\"id\":1,\"nombre\":\"Arroz 1 kg\",\"precio\":1890.0,\"stock\":38},\"cantidad\":2,\"precio\":3780.0,\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/detalle-ventas/1\"},\"detalle-ventas\":{\"href\":\"http://localhost:8080/api/detalle-ventas\"},\"producto\":{\"href\":\"http://localhost:8080/api/productos/1\"}}}";

    public static final String USUARIO_REQUEST = "{\"username\":\"nuevoCliente\",\"correo\":\"nuevo.cliente@minimarket.local\",\"password\":\"clave123\",\"roles\":[{\"id\":2}]}";
    public static final String USUARIOS_COLLECTION = "{\"_embedded\":{\"usuarioList\":[{\"id\":2,\"username\":\"cliente\",\"correo\":\"cliente@minimarket.local\",\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/usuarios/2\"}}}]},\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/usuarios\"}}}";
    public static final String USUARIO_RESPONSE = "{\"id\":2,\"username\":\"cliente\",\"correo\":\"cliente@minimarket.local\",\"roles\":[{\"id\":2,\"nombre\":\"ROLE_CLIENTE\"}],\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/usuarios/2\"},\"usuarios\":{\"href\":\"http://localhost:8080/api/usuarios\"},\"carrito\":{\"href\":\"http://localhost:8080/api/carrito\"},\"ventas\":{\"href\":\"http://localhost:8080/api/ventas\"}}}";

    private ApiExamples() {
    }
}
