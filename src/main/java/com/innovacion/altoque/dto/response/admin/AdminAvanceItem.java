package com.innovacion.altoque.dto.response.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminAvanceItem {
    private Integer id;
    private Integer reporteId;
    private String reporteTitulo;
    private String categoria;
    private String nivelRiesgo;
    private String estadoReporte;
    private String zonaReferencia;
    private String comentario;
    private String urlFoto;
    private int porcentaje;
    private String nombreUsuario;
    private LocalDateTime fecha;
}