package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findByUsuarioIdAndLeidoFalseOrderByFechaDesc(Integer usuarioId);
}