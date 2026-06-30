package com.innovacion.altoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    @Column(unique = true, length = 8, columnDefinition = "CHAR(8)")
    private String dni;


    @Column(unique = true, length = 100)
    private String email;


    @Column(length = 255)
    private String contrasena;

    @Column(length = 15)
    private String telefono;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}