package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.MiniReporteRequest;
import com.innovacion.altoque.dto.response.AnalisisIAResponse;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.MiniReporteResponse;
import com.innovacion.altoque.model.MiniReporte;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.MiniReporteRepository;
import com.innovacion.altoque.service.AnalisisIAService;
import com.innovacion.altoque.service.CloudinaryService;
import com.innovacion.altoque.service.MiniReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mini-reportes")
@RequiredArgsConstructor
public class MiniReporteController {

    private final CloudinaryService cloudinaryService;
    private final AnalisisIAService analisisIAService;
    private final MiniReporteService miniReporteService;
    private final MiniReporteRepository miniReporteRepository;

    @PostMapping("/analizar-foto")
    public ResponseEntity<ApiResponse<AnalisisIAResponse>> analizarFoto(
            @RequestParam("foto") MultipartFile foto) {
        try {
            String urlFoto = cloudinaryService.subirFoto(foto, "evidencias");
            AnalisisIAResponse analisis = analisisIAService.analizarFoto(urlFoto);

            if (!analisis.isFallback()) {
                analisis.setUrlFoto(urlFoto);
            }

            return ResponseEntity.ok(ApiResponse.ok("Análisis completado", analisis));

        } catch (Exception e) {
            AnalisisIAResponse fallback = new AnalisisIAResponse();
            fallback.setTitulo("");
            fallback.setDescripcion("");
            fallback.setIdCategoria(0);
            fallback.setCategoriaDetectada("");
            fallback.setNivelRiesgo("BAJO");
            fallback.setConfianza(0);
            fallback.setFallback(true);
            fallback.setUrlFoto("");
            return ResponseEntity.ok(ApiResponse.ok("No se pudo analizar automáticamente", fallback));
        }
    }

    /**
     * Paso 2: El ciudadano confirma (o edita) los datos y envía el reporte.
     * La URL de la foto ya viene del paso anterior (guardada en el front).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MiniReporteResponse>> crear(
            @Valid @RequestBody MiniReporteRequest req,
            @RequestParam("urlFoto") String urlFoto,
            @AuthenticationPrincipal Usuario usuario) {

        MiniReporte mini = miniReporteService.guardar(req, urlFoto, usuario);
        return ResponseEntity.ok(ApiResponse.ok("Reporte enviado correctamente",
                miniReporteService.toResponse(mini)));
    }

    @GetMapping("/mis")
    public ResponseEntity<ApiResponse<List<MiniReporteResponse>>> misMiniReportes(
            @AuthenticationPrincipal Usuario usuario) {

        List<MiniReporteResponse> lista = miniReporteRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(miniReporteService::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("OK", lista));
    }
}