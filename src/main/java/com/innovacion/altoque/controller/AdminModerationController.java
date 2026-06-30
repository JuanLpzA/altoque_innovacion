package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.admin.AdminMiniReporteItem;
import com.innovacion.altoque.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/moderacion")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminService adminService;

    @GetMapping("/mini-reportes")
    public ResponseEntity<ApiResponse<List<AdminMiniReporteItem>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("OK", adminService.listarMiniReportesPendientes(50)));
    }

    @GetMapping("/mini-reportes/{id}")
    public ResponseEntity<ApiResponse<AdminMiniReporteItem>> detalle(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", adminService.obtenerMiniReportePorId(id)));
    }

    @DeleteMapping("/mini-reportes/{id}")
    public ResponseEntity<ApiResponse<String>> cancelar(@PathVariable Integer id) {
        adminService.cancelarMiniReporte(id);
        return ResponseEntity.ok(ApiResponse.ok("Mini reporte cancelado", null));
    }
}