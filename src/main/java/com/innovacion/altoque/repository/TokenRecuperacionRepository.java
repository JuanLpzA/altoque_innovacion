package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Integer> {
    Optional<TokenRecuperacion> findByToken(String token);
}