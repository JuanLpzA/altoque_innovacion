package com.innovacion.altoque.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "niveles_riesgo")
public class NivelRiesgo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String nombre;
}