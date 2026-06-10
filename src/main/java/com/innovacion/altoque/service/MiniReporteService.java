package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.request.MiniReporteRequest;
import com.innovacion.altoque.dto.response.MiniReporteResponse;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MiniReporteService {

    private final MiniReporteRepository miniReporteRepository;
    private final ReporteRepository reporteRepository;
    private final ReporteMiniReporteRepository reporteMiniReporteRepository;
    private final CategoriaRepository categoriaRepository;
    private final NivelRiesgoRepository nivelRiesgoRepository;
    private final EstadoReporteRepository estadoReporteRepository;
    private static final int UMBRAL_AGRUPACION = 3; // mini reportes para crear uno consolidado

    @Transactional
    public MiniReporte guardar(MiniReporteRequest req, String urlFoto, Usuario usuario) {
        Categoria categoria = categoriaRepository.findById(req.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        NivelRiesgo nivelRiesgo = nivelRiesgoRepository.findById(req.getIdNivelRiesgo())
                .orElseThrow(() -> new RuntimeException("Nivel de riesgo no encontrado"));

        MiniReporte mini = new MiniReporte();
        mini.setUsuario(usuario);
        mini.setCategoria(categoria);
        mini.setNivelRiesgo(nivelRiesgo);
        mini.setTitulo(req.getTitulo());
        mini.setDescripcion(req.getDescripcion());
        mini.setUrlFoto(urlFoto);
        mini.setLatitud(req.getLatitud());
        mini.setLongitud(req.getLongitud());
        mini.setDireccionAprox(req.getDireccionAprox());
        miniReporteRepository.save(mini);

        // Lógica de agrupación:
        // Si es ALTO riesgo crea reporte consolidado inmediatamente aunque sea 1 solo
        // Si es BAJO/MEDIO agrupa cuando hay 3 o más cerca de la misma categoría
        // Hazme acuerdo de planificar cuantos mini reportes medios se necesitan para hacer un reporte real, lo coordinamos luego
        boolean esAltoRiesgo = nivelRiesgo.getNombre().equalsIgnoreCase("ALTO");

        List<MiniReporte> cercanos = miniReporteRepository.findCercanosSinAgrupar(
                req.getLatitud(), req.getLongitud(), categoria.getId()
        );

        if (esAltoRiesgo || cercanos.size() >= UMBRAL_AGRUPACION) {
            crearReporteConsolidado(cercanos, categoria, nivelRiesgo);
        }

        return mini;
    }

    private void crearReporteConsolidado(List<MiniReporte> miniReportes,
                                         Categoria categoria,
                                         NivelRiesgo nivelRiesgo) {
        EstadoReporte estadoPendiente = estadoReporteRepository
                .findByNombreIgnoreCase("pendiente")
                .orElseThrow();

        // Calcular el centro geográfico promedio
        double latProm = miniReportes.stream()
                .mapToDouble(m -> m.getLatitud().doubleValue()).average().orElse(0);
        double lngProm = miniReportes.stream()
                .mapToDouble(m -> m.getLongitud().doubleValue()).average().orElse(0);

        String descripcionConsolidada = miniReportes.stream()
                .map(m -> "- " + m.getTitulo() + ": " + m.getDescripcion())
                .reduce("", (a, b) -> a + "\n" + b);

        Reporte reporte = new Reporte();
        reporte.setCategoria(categoria);
        reporte.setNivelRiesgo(nivelRiesgo);
        reporte.setEstado(estadoPendiente);
        reporte.setTitulo("Incidencia consolidada: " + categoria.getNombre());
        reporte.setDescripcionConsolidada(descripcionConsolidada.trim());
        reporte.setLatitudCentro(new java.math.BigDecimal(latProm));
        reporte.setLongitudCentro(new java.math.BigDecimal(lngProm));
        reporte.setFechaActualizacion(LocalDateTime.now());
        miniReportes.stream()
                .map(MiniReporte::getDireccionAprox)
                .filter(d -> d != null && !d.isBlank())
                .findFirst()
                .ifPresent(reporte::setZonaReferencia);
        reporteRepository.save(reporte);

        // Marcamos los mini reportes como agrupados y los vinculamos
        for (MiniReporte mini : miniReportes) {
            mini.setAgrupado(true);
            miniReporteRepository.save(mini);

            ReporteMiniReporte rel = new ReporteMiniReporte();
            rel.setReporte(reporte);
            rel.setMiniReporte(mini);
            reporteMiniReporteRepository.save(rel);
        }
    }

    public MiniReporteResponse toResponse(MiniReporte m) {
        MiniReporteResponse r = new MiniReporteResponse();
        r.setId(m.getId());
        r.setTitulo(m.getTitulo());
        r.setDescripcion(m.getDescripcion());
        r.setCategoria(m.getCategoria().getNombre());
        r.setNivelRiesgo(m.getNivelRiesgo().getNombre());
        r.setUrlFoto(m.getUrlFoto());
        r.setLatitud(m.getLatitud());
        r.setLongitud(m.getLongitud());
        r.setDireccionAprox(m.getDireccionAprox());
        r.setFechaCreacion(m.getFechaCreacion());
        return r;
    }
}