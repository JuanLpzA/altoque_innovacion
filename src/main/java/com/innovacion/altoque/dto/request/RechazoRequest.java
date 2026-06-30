package com.innovacion.altoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RechazoRequest {

    @NotEmpty(message = "Debes seleccionar al menos un motivo de rechazo")
    private List<String> motivos;

    @NotBlank(message = "La observación es obligatoria para rechazar un reporte")
    private String observacion;
}