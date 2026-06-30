package com.innovacion.altoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambioEstadoRequest {
    @NotBlank
    private String estado;
    private String comentario;
    private Integer porcentaje;
}