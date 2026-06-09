package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    List<Reporte> findAllByOrderByFechaCreacionDesc();

    @Query("""
        SELECT r FROM Reporte r
        WHERE ABS(r.latitudCentro - :lat) < 0.009
        AND ABS(r.longitudCentro - :lng) < 0.009
        ORDER BY r.fechaCreacion DESC
    """)
    List<Reporte> findCercanos(
            @Param("lat") java.math.BigDecimal lat,
            @Param("lng") java.math.BigDecimal lng
    );
}