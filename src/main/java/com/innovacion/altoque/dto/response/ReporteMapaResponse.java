package com.innovacion.altoque.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReporteMapaResponse {
    private Integer id;
    private String titulo;
    private String categoria;
    private String nivelRiesgo;
    private String estado;
    private BigDecimal latitudCentro;
    private BigDecimal longitudCentro;
    private String zonaReferencia;
    private Integer porcentajeAvance;
    private Long totalMiniReportes;
    private LocalDateTime fechaCreacion;
}