package com.innovacion.altoque.dto.response.admin;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AdminDashboardStats {
    private long totalUltimoMes;
    private long pendientes;
    private long enProceso;
    private long resueltos;
    private Map<String, Long> porEstado;
    private Map<String, Long> porCategoria;
    private List<ReporteMapaItem> reportesParaMapa;

    @Data
    public static class ReporteMapaItem {
        private Integer id;
        private String titulo;
        private String estado;
        private String categoria;
        private java.math.BigDecimal latitud;
        private java.math.BigDecimal longitud;
    }
}