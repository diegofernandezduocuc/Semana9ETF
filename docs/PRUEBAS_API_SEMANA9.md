# Matriz de validación de la API — Semana 9

## Comandos de verificación

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

## Resultados esperados

| Área | Verificación | Resultado esperado |
|---|---|---|
| Autenticación | Login válido | `200` y token JWT |
| Autenticación | Token ausente o alterado | `401` |
| Roles | CLIENTE en recurso ADMIN | `403` |
| Productos | Lectura autenticada | `200` |
| Carrito | Acceso a recurso propio | `200` |
| Carrito | Acceso a recurso ajeno | `403` |
| Inventario | Entrada o salida válida | `201` |
| Inventario | Stock insuficiente | `409` sin cambios parciales |
| Ventas | Venta válida | `201` |
| Ventas | Error en un detalle | rollback completo |
| Sucursales | Listado de sucursales | `200` |
| Stock por sucursal | Consulta de disponibilidad | `200` |
| Stock por sucursal | Movimiento ADMIN | `201` |
| Órdenes | Generación al alcanzar mínimo | orden `PENDIENTE` automática |
| Órdenes | Recepción | `200` y reposición de stock |
| Promociones | Consulta de promociones activas | `200` |
| Pedidos | Retiro en tienda válido | `201` |
| Pedidos | Despacho sin dirección | `400` sin modificar stock |
| Pedidos | Producto inexistente | `404` sin persistencia parcial |
| Reportes | ADMIN | `200` |
| Reportes | CLIENTE | `403` |
| OpenAPI | `/v3/api-docs` | `200`, 40 rutas y 65 operaciones documentadas |
| Swagger UI | `/swagger-ui/index.html` | `200` |
| H2 | `/h2-console/` | acceso público bloqueado |

## Endpoints ampliados

- `/api/sucursales`
- `/api/stock-sucursales`
- `/api/proveedores`
- `/api/ordenes-compra`
- `/api/pedidos`
- `/api/promociones`
- `/api/reportes`

## Documentación hipermedia

Las respuestas de sucursales, stock por sucursal, promociones, pedidos y órdenes incluyen enlaces HATEOAS cuando corresponde. El contrato OpenAPI documenta los códigos de creación `201`, eliminación `204` y los errores funcionales relevantes (`400`, `401`, `403`, `404` y `409`).
