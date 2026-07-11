package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/configuracion")
@RequiredArgsConstructor
public class AdminConfiguracionController {
    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> obtener() {
        return ResponseEntity.ok(ApiResponse.ok("OK", configuracionService.obtenerTodo()));
    }

    @PutMapping("/{clave}")
    public ResponseEntity<ApiResponse<Void>> actualizar(@PathVariable String clave,
                                                        @RequestBody Map<String, String> body) {
        String valor = body.get("valor");
        if (valor == null || valor.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Valor requerido"));
        configuracionService.actualizar(clave, valor);
        return ResponseEntity.ok(ApiResponse.ok("Configuración actualizada", null));
    }
}