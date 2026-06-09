package com.innovacion.altoque.dto.response;

import lombok.Data;

@Data
public class AnalisisIAResponse {
    private String titulo;
    private String descripcion;
    private Integer idCategoria;
    private String categoriaDetectada;
    private String nivelRiesgo;
    private double confianza;
    private boolean fallback;
    private String urlFoto;
}