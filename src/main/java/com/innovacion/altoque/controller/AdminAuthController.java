package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.AdminLoginRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.JwtResponse;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.UsuarioRepository;
import com.innovacion.altoque.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody AdminLoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(req.getEmail())
                .orElse(null);

        if (usuario == null || !usuario.getActivo()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Credenciales incorrectas"));
        }

        if (!"municipalidad".equalsIgnoreCase(usuario.getRol().getNombre())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Sin acceso al panel municipal"));
        }

        if (!passwordEncoder.matches(req.getContrasena(), usuario.getContrasena())) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Credenciales incorrectas"));
        }

        String token = jwtUtil.generarToken(usuario.getDni(), usuario.getRol().getNombre());
        JwtResponse jwt = new JwtResponse(token, usuario.getNombre(),
                usuario.getApellido(), usuario.getRol().getNombre());
        return ResponseEntity.ok(ApiResponse.ok("Sesión iniciada", jwt));
    }
}