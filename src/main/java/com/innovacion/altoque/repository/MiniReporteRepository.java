package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.MiniReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MiniReporteRepository extends JpaRepository<MiniReporte, Integer> {

    List<MiniReporte> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);

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
}