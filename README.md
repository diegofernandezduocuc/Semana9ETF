# MiniMarket Plus — EFT Semana 9

Backend REST desarrollado para la Evaluación Final Transversal de Desarrollo Backend II. Integra autenticación JWT, autorización por roles, OpenAPI, HATEOAS, pruebas automatizadas y operaciones transaccionales de inventario, ventas y pedidos.

## Tecnologías

- Java 17
- Spring Boot 3.4.1
- Maven Wrapper 3.9.9
- Spring Security y OAuth2 Resource Server
- Spring Data JPA y H2
- Spring HATEOAS
- springdoc-openapi 2.8.14
- JUnit 5, MockMvc y JaCoCo

## Módulos implementados

- Usuarios, roles y registro seguro.
- Productos y categorías.
- Carrito con control de propietario.
- Inventario central y ventas transaccionales.
- Sucursales y stock por sucursal.
- Proveedores y órdenes automáticas de compra.
- Pedidos para retiro en tienda o despacho.
- Promociones vigentes.
- Reportes de rotación, stock y reposición.

## Seguridad

- Autenticación mediante JWT Bearer.
- Roles `ROLE_ADMIN`, `ROLE_CLIENTE` y `ROLE_CAJERO`.
- Contraseñas cifradas con BCrypt.
- Registro público limitado a clientes.
- Control de acceso por rol y propiedad de recursos.
- Consola H2 deshabilitada.
- Secreto JWT externalizable mediante `JWT_SECRET`.

## Ejecución local

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

## Recursos locales

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Informe JaCoCo: `target/site/jacoco/index.html`

## Credenciales de demostración

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` |
| `cliente` | `cliente123` | `ROLE_CLIENTE` |
| `cajero` | `cajero123` | `ROLE_CAJERO` |

## Documentación

- Contrato OpenAPI con 40 rutas y 65 operaciones documentadas: `docs/openapi-semana9.json`
- Colección Postman: `docs/MiniMarketPlus_Semana9.postman_collection.json`
- Matriz de validación: `docs/PRUEBAS_API_SEMANA9.md`

## Validación

- 66 pruebas automatizadas aprobadas.
- 0 fallos, 0 errores y 0 pruebas omitidas.
- Cobertura JaCoCo: 65 % de instrucciones, aproximadamente 71 % de líneas y 41 % de ramas.
- 72 clases analizadas por JaCoCo.
- Operaciones OpenAPI con resumen, descripción y códigos HTTP esperados.
