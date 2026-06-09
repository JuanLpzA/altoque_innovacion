package com.innovacion.altoque.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MiniReporteRequest {
    @NotBlank
    @Size(max = 200)
    private String titulo;

    @NotBlank
    private String descripcion;

    @NotNull
    private Integer idCategoria;

    @NotNull
    private Integer idNivelRiesgo;

    @NotNull
    private BigDecimal latitud;

    @NotNull
    private BigDecimal longitud;

    private String direccionAprox;
}