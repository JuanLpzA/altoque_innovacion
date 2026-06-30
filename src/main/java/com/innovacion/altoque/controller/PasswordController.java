package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.EstablecerContrasenaRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.service.UsuarioAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/password")
@RequiredArgsConstructor
public class PasswordController {

    private final UsuarioAdminService usuarioAdminService;

    @PostMapping("/establecer")
    public ResponseEntity<ApiResponse<String>> establecer(@Valid @RequestBody EstablecerContrasenaRequest req) {
        try {
            usuarioAdminService.establecerContrasenaPorToken(req.getToken(), req.getContrasena());
            return ResponseEntity.ok(ApiResponse.ok("Contraseña establecida correctamente. Ya puedes iniciar sesión.", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}