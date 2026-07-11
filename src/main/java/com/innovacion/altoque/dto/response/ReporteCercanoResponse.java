package com.innovacion.altoque.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReporteCercanoResponse {
    private Integer id;
    private String titulo;
    private String categoria;
    private String nivelRiesgo;
    private String estado;
    private String fotoPrincipal;
    private String zonaReferencia;
    private double distanciaMetros;
    private long totalPersonas;
    private LocalDateTime fechaCreacion;
}