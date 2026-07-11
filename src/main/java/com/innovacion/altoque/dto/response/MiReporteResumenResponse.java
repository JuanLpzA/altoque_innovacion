package com.innovacion.altoque.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MiReporteResumenResponse {
    private Integer id;
    private String titulo;
    private String categoria;
    private String nivelRiesgo;
    private String estado;
    private int porcentajeAvance;
    private String fotoPrincipal;
    private long totalPersonas;
    private LocalDateTime fechaCreacion;
}