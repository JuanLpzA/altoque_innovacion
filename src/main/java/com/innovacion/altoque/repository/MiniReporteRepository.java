package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.MiniReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MiniReporteRepository extends JpaRepository<MiniReporte, Integer> {

    List<MiniReporte> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);

    @Query("""
        SELECT m FROM MiniReporte m
        WHERE m.agrupado = false
        AND m.categoria.id = :categoriaId
        AND ABS(m.latitud - :lat) < :radio
        AND ABS(m.longitud - :lng) < :radio
        AND m.fechaCreacion >= :desde
    """)
    List<MiniReporte> findCercanosSinAgrupar(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("categoriaId") Integer categoriaId,
            @Param("radio") BigDecimal radio,
            @Param("desde") LocalDateTime desde
    );

    long countByUsuarioIdAndFechaCreacionAfter(Integer usuarioId, LocalDateTime desde);

    @Query("""
                SELECT m FROM MiniReporte m
                WHERE m.agrupado = false
                AND m.categoria.id = :categoriaId
                AND ABS(m.latitud - :lat) < 0.0009
                AND ABS(m.longitud - :lng) < 0.0009
            """)
    List<MiniReporte> findCercanosSinAgrupar(
            @Param("lat") java.math.BigDecimal lat,
            @Param("lng") java.math.BigDecimal lng,
            @Param("categoriaId") Integer categoriaId
    );

    List<MiniReporte> findAllByOrderByFechaCreacionDesc();

    @Query("""
                SELECT m FROM MiniReporte m
                WHERE m.agrupado = false
                ORDER BY m.fechaCreacion DESC
            """)
    List<MiniReporte> findNoAgrupadosRecientes(Pageable pageable);

    long countByUsuarioId(Integer usuarioId);



    @Query("""
        SELECT m FROM MiniReporte m
        WHERE (:desde IS NULL OR m.fechaCreacion >= :desde)
        AND (:hasta IS NULL OR m.fechaCreacion <= :hasta)
        AND (:categoriaId IS NULL OR m.categoria.id = :categoriaId)
        AND (:nivelRiesgoId IS NULL OR m.nivelRiesgo.id = :nivelRiesgoId)
    """)
    List<MiniReporte> findConFiltros(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("categoriaId") Integer categoriaId,
            @Param("nivelRiesgoId") Integer nivelRiesgoId
    );
}