package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.admin.AdminAvanceItem;
import com.innovacion.altoque.dto.response.admin.AdminEvidenciaResumen;
import com.innovacion.altoque.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/evidencias")
@RequiredArgsConstructor
public class AdminEvidenciaController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminAvanceItem>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("OK", adminService.listarEvidencias()));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<AdminEvidenciaResumen>> resumen() {
        return ResponseEntity.ok(ApiResponse.ok("OK", adminService.obtenerResumenAvances()));
    }
}