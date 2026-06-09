package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.Reporte;
import com.innovacion.altoque.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteRepository reporteRepository;

    @GetMapping("/cercanos")
    public ResponseEntity<ApiResponse<List<Reporte>>> cercanos(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {

        List<Reporte> lista = reporteRepository.findCercanos(lat, lng);
        return ResponseEntity.ok(ApiResponse.ok("OK", lista));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Reporte>>> todos() {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                reporteRepository.findAllByOrderByFechaCreacionDesc()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Reporte>> detalle(@PathVariable Integer id) {
        Reporte r = reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
        return ResponseEntity.ok(ApiResponse.ok("OK", r));
    }
}