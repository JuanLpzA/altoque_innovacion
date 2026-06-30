package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.LoginRequest;
import com.innovacion.altoque.dto.request.RegistroRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.JwtResponse;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.service.UsuarioService;
import com.innovacion.altoque.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<String>> registro(@Valid @RequestBody RegistroRequest req) {
        try {
            usuarioService.registrar(req);
            return ResponseEntity.ok(ApiResponse.ok("Registro exitoso", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest req) {
        Usuario usuario = usuarioService.buscarPorDni(req.getDni());

        if (!usuario.getActivo()) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Cuenta desactivada"));
        }
        if (usuario.getContrasena() == null ||
                !passwordEncoder.matches(req.getContrasena(), usuario.getContrasena())) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("DNI o contraseña incorrectos"));
        }

        String token = jwtUtil.generarToken(usuario.getId(), usuario.getRol().getNombre());
        JwtResponse jwt = new JwtResponse(
                token,
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol().getNombre()
        );
        return ResponseEntity.ok(ApiResponse.ok("Sesión iniciada", jwt));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(jakarta.servlet.http.HttpServletRequest request,
                                                      jakarta.servlet.http.HttpServletResponse response) {

        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
                    .logout(request, response, auth);
        }

        return ResponseEntity.ok(ApiResponse.ok("Sesion cerrada exitosamente", null));
    }

    @GetMapping("/verificar-estado/{dni}")
    public ResponseEntity<ApiResponse<Object>> consultarDni(@PathVariable String dni) {
        if (usuarioService.existePorDni(dni)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El DNI ya está registrado. Por favor, verifique su identidad."));
        }

        return ResponseEntity.ok(ApiResponse.ok("DNI disponible para registro", null));
    }
}