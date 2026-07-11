package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Query("""
                SELECT r FROM Reporte r
                WHERE r.fechaCreacion >= :desde
                ORDER BY r.fechaCreacion DESC
            """)
    List<Reporte> findByFechaCreacionAfter(@Param("desde") LocalDateTime desde);

    @Query("""
                SELECT r.estado.nombre, COUNT(r) FROM Reporte r
                GROUP BY r.estado.nombre
            """)
    List<Object[]> countByEstado();

    @Query("""
                SELECT r.categoria.nombre, COUNT(r) FROM Reporte r
                GROUP BY r.categoria.nombre
            """)
    List<Object[]> countByCategoria();


    @Query("""
        SELECT r FROM Reporte r
        WHERE r.categoria.id = :categoriaId
        AND r.estado.nombre IN ('pendiente','en proceso')
        AND ABS(r.latitudCentro - :lat) < :radio
        AND ABS(r.longitudCentro - :lng) < :radio
        AND r.fechaCreacion >= :desde
        ORDER BY r.fechaCreacion DESC
    """)
    List<Reporte> findAbiertosCercanos(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("categoriaId") Integer categoriaId,
            @Param("radio") BigDecimal radio,
            @Param("desde") LocalDateTime desde
    );



    @Query("""
        SELECT r FROM Reporte r
        WHERE ABS(r.latitudCentro - :lat) < :radioGrados
        AND ABS(r.longitudCentro - :lng) < :radioGrados
    """)
    List<Reporte> findEnRadio(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radioGrados") BigDecimal radioGrados
    );




    @Query("""
        SELECT DISTINCT rm.reporte FROM ReporteMiniReporte rm
        WHERE rm.miniReporte.usuario.id = :usuarioId
        ORDER BY rm.reporte.fechaCreacion DESC
    """)
    List<Reporte> findDistinctByUsuarioId(@Param("usuarioId") Integer usuarioId);
}