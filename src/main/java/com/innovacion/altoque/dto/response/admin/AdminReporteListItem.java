package com.innovacion.altoque.dto.response.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminReporteListItem {
    private Integer id;
    private String titulo;
    private String categoria;
    private String nivelRiesgo;
    private String estado;
    private int porcentajeAvance;
    private String zonaReferencia;
    private String fotoPrincipal;
    private LocalDateTime fechaCreacion;
    private int totalMiniReportes;
    private java.math.BigDecimal latitudCentro;
    private java.math.BigDecimal longitudCentro;
    private List<MiniReporteResumen> miniReportes; // 👈 nuevo

    @Data
    public static class MiniReporteResumen {
        private Integer id;
        private String nombreUsuario;
        private String titulo;
    }
}