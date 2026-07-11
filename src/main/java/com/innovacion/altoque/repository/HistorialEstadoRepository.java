package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Integer> {

    @Query("""
        SELECT h FROM HistorialEstado h
        WHERE h.reporte.id IN :idsReportes
        AND LOWER(h.estado.nombre) = 'resuelto'
    """)
    List<HistorialEstado> findResueltosPorReportes(@Param("idsReportes") List<Integer> idsReportes);
}