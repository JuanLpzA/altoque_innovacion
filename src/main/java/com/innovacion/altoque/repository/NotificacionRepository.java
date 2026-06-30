package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findByUsuarioIdAndLeidoFalseOrderByFechaDesc(Integer usuarioId);

    @Query("""
                SELECT n FROM Notificacion n
                LEFT JOIN FETCH n.reporte
                WHERE n.usuario.id = :usuarioId
                ORDER BY n.fecha DESC
            """)
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(@Param("usuarioId") Integer usuarioId);

    long countByUsuarioIdAndLeidoFalse(Integer usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true WHERE n.usuario.id = :usuarioId AND n.leido = false")
    void marcarTodasComoLeidas(@Param("usuarioId") Integer usuarioId);
}