package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.AvanceReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvanceReporteRepository extends JpaRepository<AvanceReporte, Integer> {
    List<AvanceReporte> findByReporteIdOrderByFechaDesc(Integer reporteId);
    List<AvanceReporte> findAllByOrderByFechaDesc();

    @Query("""
            SELECT a FROM AvanceReporte a
            JOIN FETCH a.usuario
            WHERE a.reporte.id IN :idsReportes
            ORDER BY a.fecha DESC
        """)
    List<AvanceReporte> findByReporteIdInWithUsuario(@Param("idsReportes") List<Integer> idsReportes);
}