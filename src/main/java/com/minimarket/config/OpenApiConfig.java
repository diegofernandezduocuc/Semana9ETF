package com.minimarket.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "MiniMarket Plus API",
                version = "2.0.0",
                description = "API REST de MiniMarket Plus para autenticación, catálogo, carritos, inventario, ventas, sucursales, stock por sucursal, proveedores, órdenes de compra, pedidos, promociones y reportes. Protegida mediante JWT y enriquecida con enlaces HATEOAS.",
                contact = @Contact(name = "Equipo MiniMarket Plus")
        ),
        servers = @Server(url = "http://localhost:8080", description = "Servidor local"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Ingrese únicamente el token generado por POST /api/auth/login."
)
public class OpenApiConfig {
}
