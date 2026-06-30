package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.PerfilResponse;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.MiniReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioPerfilController {

    private final MiniReporteRepository miniReporteRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PerfilResponse>> miPerfil(@AuthenticationPrincipal Usuario usuario) {
        PerfilResponse dto = new PerfilResponse();
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setDni(usuario.getDni());
        dto.setTotalReportes(miniReporteRepository.countByUsuarioId(usuario.getId()));
        return ResponseEntity.ok(ApiResponse.ok("OK", dto));
    }
}