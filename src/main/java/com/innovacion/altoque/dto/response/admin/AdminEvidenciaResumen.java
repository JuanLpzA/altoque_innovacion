package com.innovacion.altoque.dto.response.admin;

import lombok.Data;
import java.util.List;

@Data
public class AdminEvidenciaResumen {
    private long totalAvances;
    private long avancesUltimos7Dias;
    private long totalReportesActivos;
    private long promedioPorcentajeActivos;
    private List<ReporteAtrasadoItem> reportesAtrasados;

    @Data
    public static class ReporteAtrasadoItem {
        private Integer reporteId;
        private String titulo;
        private String categoria;
        private String nivelRiesgo;
        private String estado;
        private int porcentajeAvance;
        private long diasSinAvance;
        private String zonaReferencia;
    }
}