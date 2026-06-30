package com.innovacion.altoque.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CrearCuentaMunicipalRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank @Email
    private String email;

    private String telefono;

    @NotBlank
    @Pattern(regexp = "municipalidad_admin|municipalidad_operador",
            message = "El rol debe ser municipalidad_admin o municipalidad_operador")
    private String rol;
}