package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.admin.AdminEstadisticasResponse;
import com.innovacion.altoque.service.AdminEstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/estadisticas")
@RequiredArgsConstructor
public class AdminEstadisticasController {

    private final AdminEstadisticasService estadisticasService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminEstadisticasResponse>> obtener(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer nivelRiesgoId,
            @RequestParam(required = false) Integer estadoId) {

        LocalDateTime desdeDt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.atTime(23, 59, 59) : null;

        return ResponseEntity.ok(ApiResponse.ok("OK",
                estadisticasService.obtener(desdeDt, hastaDt, categoriaId, nivelRiesgoId, estadoId)));
    }
}