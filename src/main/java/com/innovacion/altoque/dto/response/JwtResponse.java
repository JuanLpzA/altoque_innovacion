package com.innovacion.altoque.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String nombre;
    private String apellido;
    private String rol;
}