package com.minimarket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Público", description = "Endpoint público para comprobar el estado del servicio.")
public class HolaMundoController {

    @GetMapping("/public/hola")
    @Operation(summary = "Comprobar disponibilidad", security = {})
    public String holaMundo() {
        return "MiniMarket Plus está disponible";
    }
}
