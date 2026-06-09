package com.innovacion.altoque.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "reporte_mini_reportes")
public class ReporteMiniReporte {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reporte", nullable = false)
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mini_reporte", nullable = false)
    private MiniReporte miniReporte;
}