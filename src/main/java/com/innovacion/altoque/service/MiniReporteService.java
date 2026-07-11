package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.request.MiniReporteRequest;
import com.innovacion.altoque.dto.response.MiniReporteResponse;
import com.innovacion.altoque.exception.LimiteExcedidoException;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import com.innovacion.altoque.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MiniReporteService {

    private final MiniReporteRepository miniReporteRepository;
    private final ReporteRepository reporteRepository;
    private final ReporteMiniReporteRepository reporteMiniReporteRepository;
    private final CategoriaRepository categoriaRepository;
    private final NivelRiesgoRepository nivelRiesgoRepository;
    private final EstadoReporteRepository estadoReporteRepository;
    private final ConfiguracionService configuracionService;
    private final ReporteService reporteService;

    @Transactional
    public MiniReporte guardar(MiniReporteRequest req, String urlFoto, Usuario usuario) {
        validarLimites(usuario);
        validarUbicacion(req);

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

        double lat = req.getLatitud().doubleValue();
        double lng = req.getLongitud().doubleValue();
        int radioMetros = configuracionService.getInt("radio_agrupacion_metros");
        int diasVigencia = configuracionService.getInt("dias_vigencia_reporte_abierto");
        BigDecimal radioGrados = BigDecimal.valueOf(radioMetros / 111320.0);
        LocalDateTime desde = LocalDateTime.now().minusDays(diasVigencia);

        Reporte reporteExistente = reporteRepository
                .findAbiertosCercanos(req.getLatitud(), req.getLongitud(), categoria.getId(), radioGrados, desde)
                .stream()
                .filter(r -> GeoUtils.distanciaMetros(lat, lng,
                        r.getLatitudCentro().doubleValue(), r.getLongitudCentro().doubleValue()) <= radioMetros)
                .findFirst()
                .orElse(null);

        if (reporteExistente != null) {
            adjuntarAReporteExistente(mini, reporteExistente);
            return mini;
        }

        List<MiniReporte> cercanos = miniReporteRepository
                .findCercanosSinAgrupar(req.getLatitud(), req.getLongitud(), categoria.getId(), radioGrados, desde)
                .stream()
                .filter(m -> GeoUtils.distanciaMetros(lat, lng,
                        m.getLatitud().doubleValue(), m.getLongitud().doubleValue()) <= radioMetros)
                .collect(Collectors.toList());

        boolean esAltoRiesgo = nivelRiesgo.getNombre().equalsIgnoreCase("ALTO");
        long usuariosDistintos = cercanos.stream()
                .map(m -> m.getUsuario().getId())
                .distinct()
                .count();
        int umbral = obtenerUmbral(nivelRiesgo.getNombre());

        if (esAltoRiesgo || usuariosDistintos >= umbral) {
            crearReporteConsolidado(cercanos, categoria, nivelRiesgo);
        }
        return mini;
    }

    private int obtenerUmbral(String nivel) {
        return switch (nivel.toUpperCase()) {
            case "ALTO" -> 1;
            case "MEDIO" -> 3;
            default -> 5;
        };
    }

    private void validarLimites(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        int limiteMinuto = configuracionService.getInt("limite_reportes_minuto");
        int limiteDia = configuracionService.getInt("limite_reportes_dia");

        long enUltimoMinuto = miniReporteRepository
                .countByUsuarioIdAndFechaCreacionAfter(usuario.getId(), ahora.minusMinutes(1));
        if (enUltimoMinuto >= limiteMinuto) {
            throw new LimiteExcedidoException("Debes esperar un momento antes de enviar otro reporte.");
        }

        LocalDateTime inicioDia = ahora.toLocalDate().atStartOfDay();
        long enElDia = miniReporteRepository
                .countByUsuarioIdAndFechaCreacionAfter(usuario.getId(), inicioDia);
        if (enElDia >= limiteDia) {
            throw new LimiteExcedidoException(
                    "Alcanzaste el límite de " + limiteDia + " reportes por día. Intenta mañana.");
        }
    }

    private void validarUbicacion(MiniReporteRequest req) {
        if (req.getLatitud() == null || req.getLongitud() == null) {
            throw new RuntimeException("No se pudo obtener tu ubicación. Actívala para continuar.");
        }
        double lat = req.getLatitud().doubleValue();
        double lng = req.getLongitud().doubleValue();
        if (lat < -18 || lat > 0 || lng < -82 || lng > -68) {
            throw new RuntimeException("La ubicación reportada está fuera del área de cobertura.");
        }
    }

    private void adjuntarAReporteExistente(MiniReporte mini, Reporte reporte) {
        mini.setAgrupado(true);
        miniReporteRepository.save(mini);

        ReporteMiniReporte rel = new ReporteMiniReporte();
        rel.setReporte(reporte);
        rel.setMiniReporte(mini);
        reporteMiniReporteRepository.save(rel);

        List<ReporteMiniReporte> relaciones = reporteMiniReporteRepository.findByReporteId(reporte.getId());
        double latProm = relaciones.stream()
                .mapToDouble(r -> r.getMiniReporte().getLatitud().doubleValue()).average().orElse(0);
        double lngProm = relaciones.stream()
                .mapToDouble(r -> r.getMiniReporte().getLongitud().doubleValue()).average().orElse(0);
        reporte.setLatitudCentro(BigDecimal.valueOf(latProm));
        reporte.setLongitudCentro(BigDecimal.valueOf(lngProm));
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);

        reporteService.notificarCiudadanos(reporte,
                "Se sumó una nueva evidencia a tu reporte: " + reporte.getTitulo());
    }

    private void crearReporteConsolidado(List<MiniReporte> miniReportes, Categoria categoria, NivelRiesgo nivelRiesgo) {
        EstadoReporte estadoPendiente = estadoReporteRepository
                .findByNombreIgnoreCase("pendiente")
                .orElseThrow();

        double latProm = miniReportes.stream().mapToDouble(m -> m.getLatitud().doubleValue()).average().orElse(0);
        double lngProm = miniReportes.stream().mapToDouble(m -> m.getLongitud().doubleValue()).average().orElse(0);

        String descripcionConsolidada = miniReportes.stream()
                .map(m -> "- " + m.getTitulo() + ": " + m.getDescripcion())
                .reduce("", (a, b) -> a + "\n" + b);

        Reporte reporte = new Reporte();
        reporte.setCategoria(categoria);
        reporte.setNivelRiesgo(nivelRiesgo);
        reporte.setEstado(estadoPendiente);
        reporte.setTitulo("Incidencia consolidada: " + categoria.getNombre());
        reporte.setDescripcionConsolidada(descripcionConsolidada.trim());
        reporte.setLatitudCentro(BigDecimal.valueOf(latProm));
        reporte.setLongitudCentro(BigDecimal.valueOf(lngProm));
        reporte.setFechaActualizacion(LocalDateTime.now());

        miniReportes.stream()
                .map(MiniReporte::getDireccionAprox)
                .filter(d -> d != null && !d.isBlank())
                .findFirst()
                .ifPresent(reporte::setZonaReferencia);

        reporteRepository.save(reporte);

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

    public List<MiniReporteResponse> toResponseBatch(List<MiniReporte> minis, ReporteMiniReporteRepository relRepo) {
        if (minis.isEmpty()) return List.of();
        List<Integer> ids = minis.stream().map(MiniReporte::getId).toList();
        Map<Integer, ReporteMiniReporte> relPorMini = relRepo.findByMiniReporteIdIn(ids).stream()
                .collect(Collectors.toMap(rel -> rel.getMiniReporte().getId(), rel -> rel));

        return minis.stream().map(m -> {
            MiniReporteResponse r = toResponse(m);
            ReporteMiniReporte rel = relPorMini.get(m.getId());
            if (rel != null) {
                r.setIdReporte(rel.getReporte().getId());
                r.setEstadoReporte(rel.getReporte().getEstado().getNombre());
            }
            return r;
        }).collect(Collectors.toList());
    }
}