package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.response.admin.*;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReporteRepository reporteRepository;
    private final MiniReporteRepository miniReporteRepository;
    private final ReporteMiniReporteRepository reporteMiniReporteRepository;
    private final AvanceReporteRepository avanceReporteRepository;
    private final EstadoReporteRepository estadoReporteRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final NotificacionRepository notificacionRepository;

    public AdminDashboardStats obtenerStats() {
        LocalDateTime haceUnMes = LocalDateTime.now().minusMonths(1);
        List<Reporte> todos = reporteRepository.findAllByOrderByFechaCreacionDesc();
        List<Reporte> delMes = todos.stream()
                .filter(r -> r.getFechaCreacion().isAfter(haceUnMes))
                .collect(Collectors.toList());

        long pendientes = 0, enProceso = 0, resueltos = 0;
        for (Reporte r : delMes) {
            String e = r.getEstado().getNombre().toLowerCase();
            if (e.contains("pendiente"))  pendientes++;
            else if (e.contains("proceso")) enProceso++;
            else if (e.contains("resuelto")) resueltos++;
        }

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (Object[] row : reporteRepository.countByEstado()) {
            porEstado.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> porCategoria = new LinkedHashMap<>();
        for (Object[] row : reporteRepository.countByCategoria()) {
            porCategoria.put((String) row[0], (Long) row[1]);
        }

        List<AdminDashboardStats.ReporteMapaItem> mapa = todos.stream().map(r -> {
            AdminDashboardStats.ReporteMapaItem item = new AdminDashboardStats.ReporteMapaItem();
            item.setId(r.getId());
            item.setTitulo(r.getTitulo());
            item.setEstado(r.getEstado().getNombre());
            item.setCategoria(r.getCategoria().getNombre());
            item.setLatitud(r.getLatitudCentro());
            item.setLongitud(r.getLongitudCentro());
            return item;
        }).collect(Collectors.toList());

        AdminDashboardStats stats = new AdminDashboardStats();
        stats.setTotalUltimoMes(delMes.size());
        stats.setPendientes(pendientes);
        stats.setEnProceso(enProceso);
        stats.setResueltos(resueltos);
        stats.setPorEstado(porEstado);
        stats.setPorCategoria(porCategoria);
        stats.setReportesParaMapa(mapa);
        return stats;
    }


    public List<AdminReporteListItem> listarReportes() {
        return reporteRepository.findAllByOrderByFechaCreacionDesc()
                .stream().map(this::toListItem).collect(Collectors.toList());
    }

    private AdminReporteListItem toListItem(Reporte r) {
        List<ReporteMiniReporte> rels = reporteMiniReporteRepository.findByReporteId(r.getId());

        String foto = rels.stream()
                .map(rel -> rel.getMiniReporte().getUrlFoto())
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(null);

        List<AdminReporteListItem.MiniReporteResumen> minis = rels.stream().map(rel -> {
            MiniReporte m = rel.getMiniReporte();
            AdminReporteListItem.MiniReporteResumen res = new AdminReporteListItem.MiniReporteResumen();
            res.setId(m.getId());
            res.setNombreUsuario(m.getUsuario().getNombre() + " " + m.getUsuario().getApellido());
            res.setTitulo(m.getTitulo());
            return res;
        }).collect(Collectors.toList());

        AdminReporteListItem item = new AdminReporteListItem();
        item.setId(r.getId());
        item.setTitulo(r.getTitulo());
        item.setCategoria(r.getCategoria().getNombre());
        item.setNivelRiesgo(r.getNivelRiesgo().getNombre());
        item.setEstado(r.getEstado().getNombre());
        item.setPorcentajeAvance(r.getPorcentajeAvance());
        item.setZonaReferencia(r.getZonaReferencia());
        item.setFotoPrincipal(foto);
        item.setFechaCreacion(r.getFechaCreacion());
        item.setTotalMiniReportes(rels.size());
        item.setLatitudCentro(r.getLatitudCentro());
        item.setLongitudCentro(r.getLongitudCentro());
        item.setMiniReportes(minis);
        return item;
    }

    @Transactional
    public void cambiarEstado(Integer reporteId, String nuevoEstado,
                              String comentario, Integer porcentaje, Usuario admin) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
        EstadoReporte estado = estadoReporteRepository.findByNombreIgnoreCase(nuevoEstado)
                .orElseThrow(() -> new RuntimeException("Estado no válido: " + nuevoEstado));

        reporte.setEstado(estado);
        if (porcentaje != null) {
            reporte.setPorcentajeAvance(porcentaje.shortValue());
        }
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);

        // Historial
        HistorialEstado historial = new HistorialEstado();
        historial.setReporte(reporte);
        historial.setEstado(estado);
        historial.setUsuario(admin);
        historial.setComentario(comentario);
        historialEstadoRepository.save(historial);

        // Notificar
        String msg = "Tu reporte \"" + reporte.getTitulo() + "\" cambió a estado: " + nuevoEstado;
        reporteMiniReporteRepository.findByReporteId(reporteId).forEach(rel -> {
            Notificacion noti = new Notificacion();
            noti.setUsuario(rel.getMiniReporte().getUsuario());
            noti.setReporte(reporte);
            noti.setMensaje(msg);
            notificacionRepository.save(noti);
        });
    }


    public List<AdminMiniReporteItem> listarMiniReportes() {
        return miniReporteRepository.findAllByOrderByFechaCreacionDesc()
                .stream().map(this::toMiniItem).collect(Collectors.toList());
    }

    public List<AdminMiniReporteItem> listarMiniReportesPendientes(int limit) {
        return miniReporteRepository
                .findNoAgrupadosRecientes(PageRequest.of(0, limit))
                .stream().map(this::toMiniItem).collect(Collectors.toList());
    }

    @Transactional
    public void cancelarMiniReporte(Integer miniReporteId) {
        MiniReporte mr = miniReporteRepository.findById(miniReporteId)
                .orElseThrow(() -> new RuntimeException("Mini reporte no encontrado"));
        mr.setAgrupado(true);
        miniReporteRepository.save(mr);
    }

    public List<AvanceReporte> listarEvidencias() {
        return avanceReporteRepository.findAllByOrderByFechaDesc();
    }

    private AdminMiniReporteItem toMiniItem(MiniReporte m) {
        AdminMiniReporteItem item = new AdminMiniReporteItem();
        item.setId(m.getId());
        item.setTitulo(m.getTitulo());
        item.setDescripcion(m.getDescripcion());
        item.setUrlFoto(m.getUrlFoto());
        item.setCategoria(m.getCategoria().getNombre());
        item.setNivelRiesgo(m.getNivelRiesgo().getNombre());
        item.setNombreUsuario(m.getUsuario().getNombre() + " " + m.getUsuario().getApellido());
        item.setDireccionAprox(m.getDireccionAprox());
        item.setLatitud(m.getLatitud());
        item.setLongitud(m.getLongitud());
        item.setAgrupado(m.getAgrupado());
        item.setFechaCreacion(m.getFechaCreacion());
        return item;
    }
}