package com.innovacion.altoque.dto.response.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminMiniReporteItem {
    private Integer id;
    private String titulo;
    private String descripcion;
    private String urlFoto;
    private String categoria;
    private String nivelRiesgo;
    private String nombreUsuario;
    private String direccionAprox;
    private java.math.BigDecimal latitud;
    private java.math.BigDecimal longitud;
    private boolean agrupado;
    private LocalDateTime fechaCreacion;
}