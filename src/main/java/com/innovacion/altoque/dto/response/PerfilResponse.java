package com.innovacion.altoque.dto.response;

import lombok.Data;

@Data
public class PerfilResponse {
    private String nombre;
    private String apellido;
    private String dni;
    private long totalReportes;
}