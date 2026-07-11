package com.innovacion.altoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @Column(nullable = false, length = 100)
    private String valor;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
}