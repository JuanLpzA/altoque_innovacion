package com.innovacion.altoque.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificacionResponse {
    private Integer id;
    private String mensaje;
    private Boolean leido;
    private LocalDateTime fecha;
    private Integer reporteId;
    private String reporteTitulo;
}