package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.Reniec;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReniecRepository extends JpaRepository<Reniec, Integer> {
    Optional<Reniec> findByDni(String dni);
}