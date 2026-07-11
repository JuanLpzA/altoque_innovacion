package com.innovacion.altoque.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MiniReporteResponse {
    private Integer id;
    private String titulo;
    private String descripcion;
    private String categoria;
    private String nivelRiesgo;
    private String urlFoto;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String direccionAprox;
    private LocalDateTime fechaCreacion;
    private Integer idReporte;       // null si aún no está agrupado
    private String estadoReporte;
}