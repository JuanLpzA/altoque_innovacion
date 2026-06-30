package com.innovacion.altoque.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvanceRequest {

    private String comentario;

    @NotNull(message = "El porcentaje es obligatorio")
    @Min(value = 0, message = "El porcentaje no puede ser menor a 0")
    @Max(value = 100, message = "El porcentaje no puede ser mayor a 100")
    private Short porcentaje;

    private boolean sinFoto = false;
}