package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Integer> {
    Optional<ConfiguracionSistema> findByClave(String clave);
}