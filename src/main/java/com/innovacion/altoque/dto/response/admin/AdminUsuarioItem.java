package com.innovacion.altoque.dto.response.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminUsuarioItem {
    private Integer id;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String telefono;
    private String rol;
    private boolean activo;
    private boolean pendienteActivacion;
    private LocalDateTime fechaRegistro;
    private int totalMiniReportes;
}