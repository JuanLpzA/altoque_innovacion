package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.response.MiReporteResumenResponse;
import com.innovacion.altoque.dto.response.ReporteDetalleResponse;
import com.innovacion.altoque.dto.response.ReporteMapaResponse;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import com.innovacion.altoque.dto.response.ReporteCercanoResponse;
import com.innovacion.altoque.utils.GeoUtils;
import java.math.BigDecimal;

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






    public List<ReporteCercanoResponse> obtenerTopCercanos(BigDecimal lat, BigDecimal lng, int limite) {
        double latD = lat.doubleValue();
        double lngD = lng.doubleValue();

        double[] radiosMetros = {2000, 5000, 10000, 20000};
        List<Reporte> candidatos = List.of();

        for (double radioM : radiosMetros) {
            BigDecimal radioGrados = BigDecimal.valueOf(radioM / 111320.0);
            candidatos = reporteRepository.findEnRadio(lat, lng, radioGrados);
            if (candidatos.size() >= limite) break;
        }

        if (candidatos.isEmpty()) return List.of();

        List<Integer> ids = candidatos.stream().map(Reporte::getId).toList();
        Map<Integer, Long> personasPorReporte = reporteMiniReporteRepository
                .countUsuariosDistintosPorReporte(ids)
                .stream()
                .collect(Collectors.toMap(f -> (Integer) f[0], f -> (Long) f[1]));

        List<ReporteMiniReporte> relaciones = reporteMiniReporteRepository.findByReporteIdInWithUsuario(ids);
        Map<Integer, String> fotoPorReporte = relaciones.stream()
                .filter(r -> r.getMiniReporte().getUrlFoto() != null && !r.getMiniReporte().getUrlFoto().isBlank())
                .collect(Collectors.toMap(
                        r -> r.getReporte().getId(),
                        r -> r.getMiniReporte().getUrlFoto(),
                        (a, b) -> a // si hay varias, nos quedamos con la primera
                ));

        return candidatos.stream()
                .map(r -> {
                    double distancia = GeoUtils.distanciaMetros(
                            latD, lngD, r.getLatitudCentro().doubleValue(), r.getLongitudCentro().doubleValue());
                    ReporteCercanoResponse dto = new ReporteCercanoResponse();
                    dto.setId(r.getId());
                    dto.setTitulo(r.getTitulo());
                    dto.setCategoria(r.getCategoria().getNombre());
                    dto.setNivelRiesgo(r.getNivelRiesgo().getNombre());
                    dto.setEstado(r.getEstado().getNombre());
                    dto.setFotoPrincipal(fotoPorReporte.get(r.getId()));
                    dto.setZonaReferencia(r.getZonaReferencia());
                    dto.setDistanciaMetros(Math.round(distancia));
                    dto.setTotalPersonas(personasPorReporte.getOrDefault(r.getId(), 0L));
                    dto.setFechaCreacion(r.getFechaCreacion());
                    return dto;
                })
                .sorted(Comparator.comparingDouble(ReporteCercanoResponse::getDistanciaMetros))
                .limit(limite)
                .collect(Collectors.toList());
    }




    public List<MiReporteResumenResponse> obtenerMisReportes(Integer usuarioId) {
        List<Reporte> reportes = reporteRepository.findDistinctByUsuarioId(usuarioId);
        if (reportes.isEmpty()) return List.of();

        List<Integer> ids = reportes.stream().map(Reporte::getId).toList();
        Map<Integer, Long> personasPorReporte = reporteMiniReporteRepository
                .countUsuariosDistintosPorReporte(ids).stream()
                .collect(Collectors.toMap(f -> (Integer) f[0], f -> (Long) f[1]));

        List<ReporteMiniReporte> relaciones = reporteMiniReporteRepository.findByReporteIdInWithUsuario(ids);
        Map<Integer, String> fotoPorReporte = relaciones.stream()
                .filter(r -> r.getMiniReporte().getUrlFoto() != null && !r.getMiniReporte().getUrlFoto().isBlank())
                .collect(Collectors.toMap(r -> r.getReporte().getId(), r -> r.getMiniReporte().getUrlFoto(), (a, b) -> a));

        return reportes.stream().map(r -> {
            MiReporteResumenResponse dto = new MiReporteResumenResponse();
            dto.setId(r.getId());
            dto.setTitulo(r.getTitulo());
            dto.setCategoria(r.getCategoria().getNombre());
            dto.setNivelRiesgo(r.getNivelRiesgo().getNombre());
            dto.setEstado(r.getEstado().getNombre());
            dto.setPorcentajeAvance((int) r.getPorcentajeAvance());
            dto.setFotoPrincipal(fotoPorReporte.get(r.getId()));
            dto.setTotalPersonas(personasPorReporte.getOrDefault(r.getId(), 0L));
            dto.setFechaCreacion(r.getFechaCreacion());
            return dto;
        }).collect(Collectors.toList());
    }


}