package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.response.admin.AdminEstadisticasResponse;
import com.innovacion.altoque.model.HistorialEstado;
import com.innovacion.altoque.model.MiniReporte;
import com.innovacion.altoque.model.Reporte;
import com.innovacion.altoque.repository.HistorialEstadoRepository;
import com.innovacion.altoque.repository.MiniReporteRepository;
import com.innovacion.altoque.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEstadisticasService {

    private final ReporteRepository reporteRepository;
    private final MiniReporteRepository miniReporteRepository;
    private final HistorialEstadoRepository historialEstadoRepository;

    public AdminEstadisticasResponse obtener(LocalDateTime desde, LocalDateTime hasta,
                                             Integer categoriaId, Integer nivelRiesgoId, Integer estadoId) {

        List<Reporte> reportes = reporteRepository.findConFiltros(desde, hasta, categoriaId, nivelRiesgoId, estadoId);
        List<MiniReporte> minis = miniReporteRepository.findConFiltros(desde, hasta, categoriaId, nivelRiesgoId);

        AdminEstadisticasResponse resp = new AdminEstadisticasResponse();

        resp.setTotalReportes(reportes.size());
        resp.setTotalMiniReportes(minis.size());
        long agrupados = minis.stream().filter(m -> Boolean.TRUE.equals(m.getAgrupado())).count();
        resp.setTotalMiniReportesAgrupados(agrupados);
        resp.setTasaConversion(minis.isEmpty() ? 0 : redondear(agrupados * 100.0 / minis.size()));

        resp.setPorCategoria(agrupar(reportes, r -> r.getCategoria().getNombre()));
        resp.setPorNivelRiesgo(agrupar(reportes, r -> r.getNivelRiesgo().getNombre()));
        resp.setPorEstado(agrupar(reportes, r -> r.getEstado().getNombre()));

        long resueltos = reportes.stream()
                .filter(r -> r.getEstado().getNombre().equalsIgnoreCase("resuelto"))
                .count();
        resp.setPorcentajeResueltos(reportes.isEmpty() ? 0 : redondear(resueltos * 100.0 / reportes.size()));

        resp.setTendencia(construirTendencia(reportes));

        Map<String, Double> tiempoPorCategoria = calcularTiempoResolucionPorCategoria(reportes);
        resp.setTiempoResolucionPorCategoria(tiempoPorCategoria);
        resp.setPromedioTiempoResolucionHoras(
                tiempoPorCategoria.isEmpty() ? null :
                        redondear(tiempoPorCategoria.values().stream().mapToDouble(Double::doubleValue).average().orElse(0))
        );

        resp.setTopZonas(calcularTopZonas(reportes));

        return resp;
    }

    private double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    private Map<String, Long> agrupar(List<Reporte> reportes, Function<Reporte, String> extractor) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        reportes.forEach(r -> mapa.merge(extractor.apply(r), 1L, Long::sum));
        return mapa;
    }

    private List<AdminEstadisticasResponse.PuntoTemporal> construirTendencia(List<Reporte> reportes) {
        WeekFields wf = WeekFields.ISO;
        Map<String, Long> porSemana = new TreeMap<>();
        for (Reporte r : reportes) {
            LocalDateTime f = r.getFechaCreacion();
            int anio = f.get(wf.weekBasedYear());
            int semana = f.get(wf.weekOfWeekBasedYear());
            String clave = anio + "-S" + String.format("%02d", semana);
            porSemana.merge(clave, 1L, Long::sum);
        }
        return porSemana.entrySet().stream()
                .map(e -> {
                    AdminEstadisticasResponse.PuntoTemporal p = new AdminEstadisticasResponse.PuntoTemporal();
                    p.setPeriodo(e.getKey());
                    p.setCantidad(e.getValue());
                    return p;
                }).collect(Collectors.toList());
    }

    private Map<String, Double> calcularTiempoResolucionPorCategoria(List<Reporte> reportes) {
        List<Reporte> resueltos = reportes.stream()
                .filter(r -> r.getEstado().getNombre().equalsIgnoreCase("resuelto"))
                .collect(Collectors.toList());
        if (resueltos.isEmpty()) return Map.of();

        List<Integer> ids = resueltos.stream().map(Reporte::getId).toList();
        List<HistorialEstado> historial = historialEstadoRepository.findResueltosPorReportes(ids);

        Map<Integer, LocalDateTime> fechaResolucionPorReporte = new HashMap<>();
        for (HistorialEstado h : historial) {
            fechaResolucionPorReporte.merge(h.getReporte().getId(), h.getFechaCambio(),
                    (a, b) -> a.isBefore(b) ? a : b);
        }

        Map<String, List<Double>> horasPorCategoria = new LinkedHashMap<>();
        for (Reporte r : resueltos) {
            LocalDateTime fechaResolucion = fechaResolucionPorReporte.get(r.getId());
            if (fechaResolucion == null) continue; // por si el historial no quedó registrado
            double horas = Duration.between(r.getFechaCreacion(), fechaResolucion).toMinutes() / 60.0;
            horasPorCategoria.computeIfAbsent(r.getCategoria().getNombre(), k -> new ArrayList<>()).add(horas);
        }

        Map<String, Double> resultado = new LinkedHashMap<>();
        horasPorCategoria.forEach((cat, lista) ->
                resultado.put(cat, redondear(lista.stream().mapToDouble(Double::doubleValue).average().orElse(0))));
        return resultado;
    }

    private List<AdminEstadisticasResponse.ZonaConteo> calcularTopZonas(List<Reporte> reportes) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (Reporte r : reportes) {
            String zona = r.getZonaReferencia();
            if (zona == null || zona.isBlank()) continue;
            conteo.merge(zona, 1L, Long::sum);
        }
        return conteo.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> {
                    AdminEstadisticasResponse.ZonaConteo z = new AdminEstadisticasResponse.ZonaConteo();
                    z.setZona(e.getKey());
                    z.setCantidad(e.getValue());
                    return z;
                }).collect(Collectors.toList());
    }
}