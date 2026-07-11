package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.*;
import com.innovacion.altoque.model.Reporte;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.ReporteRepository;
import com.innovacion.altoque.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        List<Reporte> reportes = reporteRepository.findCercanos(lat, lng);
        List<ReporteDetalleResponse> lista = reporteService.toDetalleResponseBatch(reportes);
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


    @GetMapping("/mapa")
    public ResponseEntity<ApiResponse<List<ReporteMapaResponse>>> paraMapa() {
        List<Reporte> reportes = reporteRepository.findAllByOrderByFechaCreacionDesc();
        List<ReporteMapaResponse> lista = reporteService.toMapaResponseBatch(reportes);
        return ResponseEntity.ok(ApiResponse.ok("OK", lista));
    }


    @GetMapping("/top-cercanos")
    public ResponseEntity<ApiResponse<List<ReporteCercanoResponse>>> topCercanos(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(ApiResponse.ok("OK", reporteService.obtenerTopCercanos(lat, lng, limite)));
    }


    @GetMapping("/mis")
    public ResponseEntity<ApiResponse<List<MiReporteResumenResponse>>> misReportes(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.ok("OK", reporteService.obtenerMisReportes(usuario.getId())));
    }
}