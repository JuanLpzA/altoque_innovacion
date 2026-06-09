package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.NivelRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NivelRiesgoRepository extends JpaRepository<NivelRiesgo, Integer> {
    Optional<NivelRiesgo> findByNombreIgnoreCase(String nombre);
}