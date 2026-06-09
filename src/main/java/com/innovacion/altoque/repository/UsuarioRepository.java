package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByDni(String dni);
    boolean existsByDni(String dni);
}