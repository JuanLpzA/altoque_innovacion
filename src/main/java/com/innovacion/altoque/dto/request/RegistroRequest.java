package com.innovacion.altoque.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroRequest {
    @NotBlank
    @Size(max = 60)
    private String nombre;

    @NotBlank
    @Size(max = 60)
    private String apellido;

    @NotBlank
    @Size(min = 8, max = 8, message = "El DNI debe tener 8 dígitos")
    @Pattern(regexp = "\\d{8}", message = "El DNI solo debe contener números")
    private String dni;

    @NotBlank
    @Size(min = 6, max = 6, message = "La contraseña debe tener 6 dígitos")
    @Pattern(regexp = "\\d{6}", message = "La contraseña solo debe contener números")
    private String contrasena;

    @Size(max = 15)
    private String telefono;
}