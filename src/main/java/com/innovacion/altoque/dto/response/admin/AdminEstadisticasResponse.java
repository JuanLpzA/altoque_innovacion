package com.innovacion.altoque.dto.response.admin;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AdminEstadisticasResponse {

    // KPIs
    private long totalReportes;
    private long totalMiniReportes;
    private long totalMiniReportesAgrupados;
    private double tasaConversion;
    private double porcentajeResueltos;
    private Double promedioTiempoResolucionHoras;

    // Distribuciones
    private Map<String, Long> porCategoria;
    private Map<String, Long> porNivelRiesgo;
    private Map<String, Long> porEstado;

    // Series
    private List<PuntoTemporal> tendencia;
    private Map<String, Double> tiempoResolucionPorCategoria;
    private List<ZonaConteo> topZonas;

    @Data
    public static class PuntoTemporal {
        private String periodo;
        private long cantidad;
    }

    @Data
    public static class ZonaConteo {
        private String zona;
        private long cantidad;
    }
}