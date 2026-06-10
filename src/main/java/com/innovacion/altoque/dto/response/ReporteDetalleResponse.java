package com.innovacion.altoque.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReporteDetalleResponse {
    private Integer id;
    private String titulo;
    private String descripcionConsolidada;
    private String categoria;
    private String nivelRiesgo;
    private String estado;
    private Integer porcentajeAvance;
    private String zonaReferencia;
    private BigDecimal latitudCentro;
    private BigDecimal longitudCentro;
    private LocalDateTime fechaCreacion;
    private String fotoPrincipal;
    private List<MiniReporteResumen> miniReportes;
    private List<AvanceResumen> avances;

    @Data
    public static class MiniReporteResumen {
        private Integer id;
        private String nombreUsuario;
        private String iniciales;
        private String colorAvatar;
        private String urlFoto;
        private LocalDateTime fechaCreacion;
    }

    @Data
    public static class AvanceResumen {
        private Integer id;
        private String comentario;
        private String urlFoto;
        private Integer porcentaje;
        private String nombreUsuario;
        private LocalDateTime fecha;
    }
}