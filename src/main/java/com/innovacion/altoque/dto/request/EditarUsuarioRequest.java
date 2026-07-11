package com.innovacion.altoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditarUsuarioRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
}