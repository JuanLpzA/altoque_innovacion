package com.innovacion.altoque.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "estados_reporte")
public class EstadoReporte {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;
}