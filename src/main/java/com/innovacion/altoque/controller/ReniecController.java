package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.Reniec;
import com.innovacion.altoque.repository.UsuarioRepository;
import com.innovacion.altoque.service.ReniecService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ReniecController {
    private final ReniecService reniecService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/consultar-dni/{dni}")
    public ResponseEntity<ApiResponse<Reniec>> consultarDni(@PathVariable String dni) {
        if (dni == null || dni.length() != 8 || !dni.matches("\\d+")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("El DNI debe tener 8 dígitos."));
        }

        if (usuarioRepository.existsByDni(dni)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Este DNI ya está registrado."));
        }

        Reniec datos = reniecService.consultarDni(dni);
        if (datos == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("No se encontraron datos."));
        }

        return ResponseEntity.ok(ApiResponse.ok("Identidad verificada", datos));
    }
}
