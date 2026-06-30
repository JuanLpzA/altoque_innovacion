package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.CrearCuentaMunicipalRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.admin.AdminUsuarioItem;
import com.innovacion.altoque.service.UsuarioAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping("/municipales")
    public ResponseEntity<ApiResponse<List<AdminUsuarioItem>>> listarMunicipales() {
        return ResponseEntity.ok(ApiResponse.ok("OK", usuarioAdminService.listarCuentasMunicipales()));
    }

    @GetMapping("/ciudadanos")
    public ResponseEntity<ApiResponse<List<AdminUsuarioItem>>> listarCiudadanos() {
        return ResponseEntity.ok(ApiResponse.ok("OK", usuarioAdminService.listarCiudadanos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUsuarioItem>> detalle(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", usuarioAdminService.obtenerPorId(id)));
    }

    @PostMapping("/municipales")
    public ResponseEntity<ApiResponse<String>> crearCuentaMunicipal(
            @Valid @RequestBody CrearCuentaMunicipalRequest req) {
        usuarioAdminService.crearCuentaMunicipal(req);
        return ResponseEntity.ok(ApiResponse.ok(
                "Cuenta creada. Se envió un correo al usuario para que establezca su contraseña.", null));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<ApiResponse<String>> cambiarActivo(
            @PathVariable Integer id, @RequestParam boolean activo) {
        usuarioAdminService.cambiarActivo(id, activo);
        return ResponseEntity.ok(ApiResponse.ok(
                activo ? "Cuenta activada" : "Cuenta desactivada", null));
    }

    @PostMapping("/{id}/resetear-contrasena")
    public ResponseEntity<ApiResponse<String>> resetearContrasena(@PathVariable Integer id) {
        usuarioAdminService.solicitarReseteoContrasena(id);
        return ResponseEntity.ok(ApiResponse.ok(
                "Se envió un correo al usuario con el enlace para restablecer su contraseña.", null));
    }
}