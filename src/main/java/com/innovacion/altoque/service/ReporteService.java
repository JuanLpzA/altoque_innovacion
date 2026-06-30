package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.response.ReporteDetalleResponse;
import com.innovacion.altoque.dto.response.ReporteMapaResponse;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final NotificacionRepository notificacionRepository;
    private final MiniReporteRepository miniReporteRepository;
    private final ReporteMiniReporteRepository reporteMiniReporteRepository;
    private final AvanceReporteRepository avanceReporteRepository;

    public void notificarCiudadanos(Reporte reporte, String mensaje) {
        List<ReporteMiniReporte> relaciones = reporteMiniReporteRepository
                .findByReporteId(reporte.getId());
        for (ReporteMiniReporte rel : relaciones) {
            Usuario ciudadano = rel.getMiniReporte().getUsuario();
            Notificacion noti = new Notificacion();
            noti.setUsuario(ciudadano);
            noti.setReporte(reporte);
            noti.setMensaje(mensaje);
            notificacionRepository.save(noti);
        }
    }

    public List<Reporte> obtenerTodos() {
        return reporteRepository.findAllByOrderByFechaCreacionDesc();
    }

    public Reporte obtenerPorId(Integer id) {
        return reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
    }

    public List<ReporteDetalleResponse> toDetalleResponseBatch(List<Reporte> reportes) {
        if (reportes.isEmpty()) return List.of();

        List<Integer> idsReportes = reportes.stream().map(Reporte::getId).toList();

        List<ReporteMiniReporte> todasLasRelaciones =
                reporteMiniReporteRepository.findByReporteIdInWithUsuario(idsReportes);

        List<AvanceReporte> todosLosAvances =
                avanceReporteRepository.findByReporteIdInWithUsuario(idsReportes);

        Map<Integer, List<ReporteMiniReporte>> relacionesPorReporte = todasLasRelaciones.stream()
                .collect(Collectors.groupingBy(r -> r.getReporte().getId()));
        Map<Integer, List<AvanceReporte>> avancesPorReporte = todosLosAvances.stream()
                .collect(Collectors.groupingBy(a -> a.getReporte().getId()));

        return reportes.stream()
                .map(r -> construirDetalle(
                        r,
                        relacionesPorReporte.getOrDefault(r.getId(), List.of()),
                        avancesPorReporte.getOrDefault(r.getId(), List.of())
                ))
                .collect(Collectors.toList());
    }

    public ReporteDetalleResponse toDetalleResponse(Reporte reporte) {
        List<ReporteMiniReporte> relaciones = reporteMiniReporteRepository
                .findByReporteId(reporte.getId());
        List<AvanceReporte> avances = avanceReporteRepository
                .findByReporteIdOrderByFechaDesc(reporte.getId());
        return construirDetalle(reporte, relaciones, avances);
    }

    private ReporteDetalleResponse construirDetalle(Reporte reporte,
                                                    List<ReporteMiniReporte> relaciones,
                                                    List<AvanceReporte> avances) {
        String fotoPrincipal = relaciones.stream()
                .map(rel -> rel.getMiniReporte().getUrlFoto())
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);

        String[] colores = {"#1a3a8f", "#0fa89a", "#ef4444", "#f59e0b", "#8b5cf6", "#ec4899"};
        List<ReporteDetalleResponse.MiniReporteResumen> miniResumenes = new ArrayList<>();
        for (int i = 0; i < relaciones.size(); i++) {
            MiniReporte m = relaciones.get(i).getMiniReporte();
            ReporteDetalleResponse.MiniReporteResumen res = new ReporteDetalleResponse.MiniReporteResumen();
            res.setId(m.getId());
            String nombre = m.getUsuario().getNombre();
            String apellido = m.getUsuario().getApellido();
            res.setNombreUsuario(nombre + " " + apellido);
            String ini = "";
            if (!nombre.isEmpty()) ini += nombre.charAt(0);
            if (!apellido.isEmpty()) ini += apellido.charAt(0);
            res.setIniciales(ini.toUpperCase());
            res.setColorAvatar(colores[i % colores.length]);
            res.setUrlFoto(m.getUrlFoto());
            res.setFechaCreacion(m.getFechaCreacion());
            miniResumenes.add(res);
        }

        List<ReporteDetalleResponse.AvanceResumen> avanceResumenes = avances.stream().map(a -> {
            ReporteDetalleResponse.AvanceResumen ar = new ReporteDetalleResponse.AvanceResumen();
            ar.setId(a.getId());
            ar.setComentario(a.getComentario());
            ar.setUrlFoto(a.getUrlFoto());
            ar.setPorcentaje((int) a.getPorcentaje());
            ar.setNombreUsuario(a.getUsuario().getNombre() + " " + a.getUsuario().getApellido());
            ar.setFecha(a.getFecha());
            return ar;
        }).collect(Collectors.toList());

        ReporteDetalleResponse dto = new ReporteDetalleResponse();
        dto.setId(reporte.getId());
        dto.setTitulo(reporte.getTitulo());
        dto.setDescripcionConsolidada(reporte.getDescripcionConsolidada());
        dto.setCategoria(reporte.getCategoria().getNombre());
        dto.setNivelRiesgo(reporte.getNivelRiesgo().getNombre());
        dto.setEstado(reporte.getEstado().getNombre());
        dto.setPorcentajeAvance((int) reporte.getPorcentajeAvance());

        String zona = reporte.getZonaReferencia();
        if (zona == null || zona.isBlank()) {
            zona = relaciones.stream()
                    .map(rel -> rel.getMiniReporte().getDireccionAprox())
                    .filter(d -> d != null && !d.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        dto.setZonaReferencia(zona);
        dto.setLatitudCentro(reporte.getLatitudCentro());
        dto.setLongitudCentro(reporte.getLongitudCentro());
        dto.setFechaCreacion(reporte.getFechaCreacion());
        dto.setFotoPrincipal(fotoPrincipal);
        dto.setMiniReportes(miniResumenes);
        dto.setAvances(avanceResumenes);
        return dto;
    }

    public List<ReporteMapaResponse> toMapaResponseBatch(List<Reporte> reportes) {
        if (reportes.isEmpty()) return List.of();

        List<Integer> idsReportes = reportes.stream().map(Reporte::getId).toList();

        Map<Integer, Long> conteoPorReporte = reporteMiniReporteRepository
                .countMiniReportesPorReporte(idsReportes)
                .stream()
                .collect(Collectors.toMap(
                        fila -> (Integer) fila[0],
                        fila -> (Long) fila[1]
                ));

        return reportes.stream().map(r -> {
            ReporteMapaResponse dto = new ReporteMapaResponse();
            dto.setId(r.getId());
            dto.setTitulo(r.getTitulo());
            dto.setCategoria(r.getCategoria().getNombre());
            dto.setNivelRiesgo(r.getNivelRiesgo().getNombre());
            dto.setEstado(r.getEstado().getNombre());
            dto.setLatitudCentro(r.getLatitudCentro());
            dto.setLongitudCentro(r.getLongitudCentro());
            dto.setZonaReferencia(r.getZonaReferencia());
            dto.setPorcentajeAvance((int) r.getPorcentajeAvance());
            dto.setTotalMiniReportes(conteoPorReporte.getOrDefault(r.getId(), 0L));
            dto.setFechaCreacion(r.getFechaCreacion());
            return dto;
        }).collect(Collectors.toList());
    }


}