package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.response.admin.*;
import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
        return obtenerStats("mes"); // compatibilidad hacia atrás
    }

    public AdminDashboardStats obtenerStats(String periodo) {
        LocalDateTime ahora = LocalDateTime.now();
        String periodoNormalizado = (periodo == null) ? "mes" : periodo.toLowerCase();
        LocalDateTime desde;
        switch (periodoNormalizado) {
            case "hoy":
                desde = ahora.toLocalDate().atStartOfDay();
                break;
            case "semana":
                desde = ahora.minusDays(7);
                break;
            case "todo":
                desde = LocalDateTime.of(2000, 1, 1, 0, 0);
                break;
            case "mes":
            default:
                desde = ahora.minusMonths(1);
                periodoNormalizado = "mes";
                break;
        }

        List<Reporte> todos = reporteRepository.findAllByOrderByFechaCreacionDesc();
        List<Reporte> delPeriodo = todos.stream()
                .filter(r -> r.getFechaCreacion().isAfter(desde))
                .collect(Collectors.toList());

        long pendientes = 0, enProceso = 0, resueltos = 0;
        for (Reporte r : delPeriodo) {
            String e = r.getEstado().getNombre().toLowerCase();
            if (e.contains("pendiente")) pendientes++;
            else if (e.contains("proceso")) enProceso++;
            else if (e.contains("resuelto")) resueltos++;
        }

        Map<String, Long> porEstado = delPeriodo.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getEstado().getNombre(), LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> porCategoria = delPeriodo.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCategoria().getNombre(), LinkedHashMap::new, Collectors.counting()));

        List<AdminDashboardStats.ReporteMapaItem> mapa = delPeriodo.stream().map(r -> {
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
        stats.setTotalUltimoMes(delPeriodo.size());
        stats.setPendientes(pendientes);
        stats.setEnProceso(enProceso);
        stats.setResueltos(resueltos);
        stats.setPorEstado(porEstado);
        stats.setPorCategoria(porCategoria);
        stats.setReportesParaMapa(mapa);
        stats.setPeriodo(periodoNormalizado);
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
    public void rechazarReporte(Integer reporteId, List<String> motivos, String observacion, Usuario admin) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        String estadoActual = reporte.getEstado().getNombre().toLowerCase();
        if (estadoActual.contains("resuelto") || estadoActual.contains("rechazado")) {
            throw new RuntimeException("No se puede rechazar un reporte que ya está " + estadoActual);
        }

        EstadoReporte estadoRechazado = estadoReporteRepository.findByNombreIgnoreCase("rechazado")
                .orElseThrow(() -> new RuntimeException("Estado 'rechazado' no configurado"));

        reporte.setEstado(estadoRechazado);
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);

        String motivosTexto = String.join("; ", motivos);
        String comentarioCompleto = "Motivos: " + motivosTexto + " | Observación: " + observacion;

        HistorialEstado historial = new HistorialEstado();
        historial.setReporte(reporte);
        historial.setEstado(estadoRechazado);
        historial.setUsuario(admin);
        historial.setComentario(comentarioCompleto);
        historialEstadoRepository.save(historial);

        String msg = "Tu reporte \"" + reporte.getTitulo() + "\" fue rechazado. Motivo: " + observacion;
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

    public AdminMiniReporteItem obtenerMiniReportePorId(Integer id) {
        MiniReporte m = miniReporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mini reporte no encontrado"));
        return toMiniItem(m);
    }

    @Transactional
    public void cancelarMiniReporte(Integer miniReporteId) {
        MiniReporte mr = miniReporteRepository.findById(miniReporteId)
                .orElseThrow(() -> new RuntimeException("Mini reporte no encontrado"));
        mr.setAgrupado(true);
        miniReporteRepository.save(mr);
    }

    public List<AdminAvanceItem> listarEvidencias() {
        return avanceReporteRepository.findAllByOrderByFechaDesc()
                .stream().map(this::toAvanceItem).collect(Collectors.toList());
    }

    private AdminAvanceItem toAvanceItem(AvanceReporte a) {
        Reporte r = a.getReporte();
        AdminAvanceItem item = new AdminAvanceItem();
        item.setId(a.getId());
        item.setReporteId(r.getId());
        item.setReporteTitulo(r.getTitulo());
        item.setCategoria(r.getCategoria().getNombre());
        item.setNivelRiesgo(r.getNivelRiesgo().getNombre());
        item.setEstadoReporte(r.getEstado().getNombre());
        item.setZonaReferencia(r.getZonaReferencia());
        item.setComentario(a.getComentario());
        item.setUrlFoto(a.getUrlFoto());
        item.setPorcentaje(a.getPorcentaje() == null ? 0 : a.getPorcentaje());
        item.setNombreUsuario(a.getUsuario().getNombre() + " " + a.getUsuario().getApellido());
        item.setFecha(a.getFecha());
        return item;
    }


    public AdminEvidenciaResumen obtenerResumenAvances() {
        final int UMBRAL_DIAS_ATRASO = 5;

        List<Reporte> activos = reporteRepository.findAllByOrderByFechaCreacionDesc().stream()
                .filter(r -> {
                    String e = r.getEstado().getNombre().toLowerCase();
                    return e.contains("pendiente") || e.contains("proceso");
                })
                .collect(Collectors.toList());

        List<AvanceReporte> todosAvances = avanceReporteRepository.findAllByOrderByFechaDesc();

        Map<Integer, LocalDateTime> ultimoAvancePorReporte = new HashMap<>();
        for (AvanceReporte a : todosAvances) {
            ultimoAvancePorReporte.merge(
                    a.getReporte().getId(), a.getFecha(),
                    (f1, f2) -> f1.isAfter(f2) ? f1 : f2);
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime haceUnaSemana = ahora.minusDays(7);
        long avancesUltimos7Dias = todosAvances.stream()
                .filter(a -> a.getFecha().isAfter(haceUnaSemana))
                .count();

        List<AdminEvidenciaResumen.ReporteAtrasadoItem> atrasados = new ArrayList<>();
        for (Reporte r : activos) {
            LocalDateTime referencia = ultimoAvancePorReporte.getOrDefault(r.getId(), r.getFechaCreacion());
            long dias = Duration.between(referencia, ahora).toDays();
            if (dias >= UMBRAL_DIAS_ATRASO) {
                AdminEvidenciaResumen.ReporteAtrasadoItem item = new AdminEvidenciaResumen.ReporteAtrasadoItem();
                item.setReporteId(r.getId());
                item.setTitulo(r.getTitulo());
                item.setCategoria(r.getCategoria().getNombre());
                item.setNivelRiesgo(r.getNivelRiesgo().getNombre());
                item.setEstado(r.getEstado().getNombre());
                item.setPorcentajeAvance(r.getPorcentajeAvance() == null ? 0 : r.getPorcentajeAvance());
                item.setDiasSinAvance(dias);
                item.setZonaReferencia(r.getZonaReferencia());
                atrasados.add(item);
            }
        }
        atrasados.sort((a, b) -> Long.compare(b.getDiasSinAvance(), a.getDiasSinAvance()));

        double promedio = activos.isEmpty() ? 0 : activos.stream()
                .mapToInt(r -> r.getPorcentajeAvance() == null ? 0 : r.getPorcentajeAvance())
                .average().orElse(0);

        AdminEvidenciaResumen resumen = new AdminEvidenciaResumen();
        resumen.setTotalAvances(todosAvances.size());
        resumen.setAvancesUltimos7Dias(avancesUltimos7Dias);
        resumen.setTotalReportesActivos(activos.size());
        resumen.setPromedioPorcentajeActivos(Math.round(promedio));
        resumen.setReportesAtrasados(atrasados);
        return resumen;
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