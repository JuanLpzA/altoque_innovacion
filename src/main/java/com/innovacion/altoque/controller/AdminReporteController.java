package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.CambioEstadoRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.ReporteDetalleResponse;
import com.innovacion.altoque.dto.response.admin.AdminReporteListItem;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.service.AdminService;
import com.innovacion.altoque.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReporteController {

    private final AdminService adminService;
    private final ReporteService reporteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminReporteListItem>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("OK", adminService.listarReportes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReporteDetalleResponse>> detalle(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                reporteService.toDetalleResponse(reporteService.obtenerPorId(id))));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<String>> cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody CambioEstadoRequest req,
            @AuthenticationPrincipal Usuario admin) {
        adminService.cambiarEstado(id, req.getEstado(), req.getComentario(),
                req.getPorcentaje(), admin);
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado", null));
    }
}