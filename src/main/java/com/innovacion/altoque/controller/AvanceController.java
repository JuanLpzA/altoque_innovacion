package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.AvanceRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import com.innovacion.altoque.service.CloudinaryService;
import com.innovacion.altoque.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public ResponseEntity<ApiResponse<String>> agregarAvance(
            @PathVariable Integer idReporte,
            @RequestParam(required = false) String comentario,
            @RequestParam Short porcentaje,
            @RequestParam(defaultValue = "false") boolean sinFoto,
            @RequestParam(required = false) MultipartFile foto,
            @AuthenticationPrincipal Usuario usuario) throws IOException {

        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        String estadoActual = reporte.getEstado().getNombre().toLowerCase();
        if (estadoActual.contains("resuelto") || estadoActual.contains("rechazado")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("No se pueden registrar avances en un reporte " + estadoActual));
        }

        // Regla: el porcentaje nunca puede bajar
        short porcentajeActual = reporte.getPorcentajeAvance() == null ? 0 : reporte.getPorcentajeAvance();
        if (porcentaje < porcentajeActual) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            "El porcentaje no puede ser menor al avance actual (" + porcentajeActual + "%)"));
        }

        // Regla: foto obligatoria salvo que se confirme explícitamente que no hay evidencia
        boolean hayFoto = foto != null && !foto.isEmpty();
        if (!hayFoto && !sinFoto) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Debes adjuntar una foto de evidencia, o confirmar que no cuentas con ella"));
        }

        String urlFoto = null;
        if (hayFoto) {
            urlFoto = cloudinaryService.subirFoto(foto, "avances");
        }

        AvanceReporte avance = new AvanceReporte();
        avance.setReporte(reporte);
        avance.setUsuario(usuario);
        avance.setComentario(comentario);
        avance.setUrlFoto(urlFoto);
        avance.setPorcentaje(porcentaje);
        avanceReporteRepository.save(avance);

        // Regla: el estado se calcula automáticamente, nunca lo elige el admin
        String nombreEstadoDestino = (porcentaje >= 100) ? "resuelto" : "en proceso";
        EstadoReporte estado = estadoReporteRepository
                .findByNombreIgnoreCase(nombreEstadoDestino)
                .orElseThrow(() -> new RuntimeException("Estado no configurado: " + nombreEstadoDestino));

        reporte.setPorcentajeAvance(porcentaje);
        reporte.setEstado(estado);
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);

        HistorialEstado historial = new HistorialEstado();
        historial.setReporte(reporte);
        historial.setEstado(estado);
        historial.setUsuario(usuario);
        historial.setComentario(comentario);
        historialEstadoRepository.save(historial);

        String mensajeNoti = "Tu reporte '" + reporte.getTitulo() + "' fue actualizado a: "
                + estado.getNombre() + " (" + porcentaje + "% completado)";
        reporteService.notificarCiudadanos(reporte, mensajeNoti);

        return ResponseEntity.ok(ApiResponse.ok("Avance registrado", null));
    }

    @GetMapping("/{idReporte}")
    public ResponseEntity<ApiResponse<List<AvanceReporte>>> listar(@PathVariable Integer idReporte) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                avanceReporteRepository.findByReporteIdOrderByFechaDesc(idReporte)));
    }
}