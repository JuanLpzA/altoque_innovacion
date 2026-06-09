package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import com.innovacion.altoque.service.CloudinaryService;
import com.innovacion.altoque.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/avances")
@RequiredArgsConstructor
public class AvanceController {

    private final AvanceReporteRepository avanceReporteRepository;
    private final ReporteRepository reporteRepository;
    private final EstadoReporteRepository estadoReporteRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final CloudinaryService cloudinaryService;
    private final ReporteService reporteService;

    @PostMapping("/{idReporte}")
    public ResponseEntity<ApiResponse<String>> agregarAvance(
            @PathVariable Integer idReporte,
            @RequestParam(required = false) String comentario,
            @RequestParam Short porcentaje,
            @RequestParam String nuevoEstado,
            @RequestParam(required = false) MultipartFile foto,
            @AuthenticationPrincipal Usuario usuario) throws IOException {

        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        String urlFoto = null;
        if (foto != null && !foto.isEmpty()) {
            urlFoto = cloudinaryService.subirFoto(foto, "avances");
        }

        AvanceReporte avance = new AvanceReporte();
        avance.setReporte(reporte);
        avance.setUsuario(usuario);
        avance.setComentario(comentario);
        avance.setUrlFoto(urlFoto);
        avance.setPorcentaje(porcentaje);
        avanceReporteRepository.save(avance);

        reporte.setPorcentajeAvance(porcentaje);
        EstadoReporte estado = estadoReporteRepository
                .findByNombreIgnoreCase(nuevoEstado)
                .orElseThrow(() -> new RuntimeException("Estado no válido"));
        reporte.setEstado(estado);
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);

        HistorialEstado historial = new HistorialEstado();
        historial.setReporte(reporte);
        historial.setEstado(estado);
        historial.setUsuario(usuario);
        historial.setComentario(comentario);
        historialEstadoRepository.save(historial);

        String mensajeNoti = "Tu reporte '" + reporte.getTitulo() + "' fue actualizado a: " + estado.getNombre()
                + " (" + porcentaje + "% completado)";
        reporteService.notificarCiudadanos(reporte, mensajeNoti);

        return ResponseEntity.ok(ApiResponse.ok("Avance registrado", null));
    }

    @GetMapping("/{idReporte}")
    public ResponseEntity<ApiResponse<List<AvanceReporte>>> listar(@PathVariable Integer idReporte) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                avanceReporteRepository.findByReporteIdOrderByFechaDesc(idReporte)));
    }
}