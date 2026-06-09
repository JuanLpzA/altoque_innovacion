package com.innovacion.altoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reportes")
public class Reporte {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nivel_riesgo", nullable = false)
    private NivelRiesgo nivelRiesgo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoReporte estado;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion_consolidada", nullable = false, columnDefinition = "TEXT")
    private String descripcionConsolidada;

    @Column(name = "latitud_centro", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitudCentro;

    @Column(name = "longitud_centro", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitudCentro;

    @Column(name = "zona_referencia", length = 255)
    private String zonaReferencia;

    @Column(name = "porcentaje_avance")
    private Short porcentajeAvance = 0;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
}