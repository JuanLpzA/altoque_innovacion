package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.ReporteDetalleResponse;
import com.innovacion.altoque.model.Reporte;
import com.innovacion.altoque.repository.ReporteRepository;
import com.innovacion.altoque.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {
    private final ReporteRepository reporteRepository;
    private final ReporteService reporteService;

    @GetMapping("/cercanos")
    public ResponseEntity<ApiResponse<List<ReporteDetalleResponse>>> cercanos(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {
        List<ReporteDetalleResponse> lista = reporteRepository.findCercanos(lat, lng)
                .stream()
                .map(reporteService::toDetalleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("OK", lista));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReporteDetalleResponse>>> todos() {
        List<ReporteDetalleResponse> lista = reporteRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .map(reporteService::toDetalleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("OK", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReporteDetalleResponse>> detalle(@PathVariable Integer id) {
        Reporte r = reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
        return ResponseEntity.ok(ApiResponse.ok("OK", reporteService.toDetalleResponse(r)));
    }
}