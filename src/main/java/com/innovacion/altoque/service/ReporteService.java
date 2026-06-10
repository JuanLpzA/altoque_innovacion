package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.response.ReporteDetalleResponse;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public ReporteDetalleResponse toDetalleResponse(Reporte reporte) {
        List<ReporteMiniReporte> relaciones = reporteMiniReporteRepository
                .findByReporteId(reporte.getId());

        String fotoPrincipal = relaciones.stream()
                .map(rel -> rel.getMiniReporte().getUrlFoto())
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);

        String[] colores = {"#1a3a8f","#0fa89a","#ef4444","#f59e0b","#8b5cf6","#ec4899"};

        List<ReporteDetalleResponse.MiniReporteResumen> miniResumenes = new java.util.ArrayList<>();
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

        List<AvanceReporte> avances = avanceReporteRepository
                .findByReporteIdOrderByFechaDesc(reporte.getId());

        List<ReporteDetalleResponse.AvanceResumen> avanceResumenes = avances.stream().map(a -> {
            ReporteDetalleResponse.AvanceResumen ar = new ReporteDetalleResponse.AvanceResumen();
            ar.setId(a.getId());
            ar.setComentario(a.getComentario());
            ar.setUrlFoto(a.getUrlFoto());
            ar.setPorcentaje((int) a.getPorcentaje());
            ar.setNombreUsuario(a.getUsuario().getNombre() + " " + a.getUsuario().getApellido());
            ar.setFecha(a.getFecha());
            return ar;
        }).collect(java.util.stream.Collectors.toList());

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
}