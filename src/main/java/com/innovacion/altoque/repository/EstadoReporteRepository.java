package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.EstadoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EstadoReporteRepository extends JpaRepository<EstadoReporte, Integer> {
    Optional<EstadoReporte> findByNombreIgnoreCase(String nombre);
}