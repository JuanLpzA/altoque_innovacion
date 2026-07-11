package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.ReporteMiniReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReporteMiniReporteRepository extends JpaRepository<ReporteMiniReporte, Integer> {

    List<ReporteMiniReporte> findByReporteId(Integer reporteId);

    @Query("""
                SELECT rm FROM ReporteMiniReporte rm
                JOIN FETCH rm.miniReporte m
                JOIN FETCH m.usuario
                WHERE rm.reporte.id IN :idsReportes
            """)
    List<ReporteMiniReporte> findByReporteIdInWithUsuario(@Param("idsReportes") List<Integer> idsReportes);


    @Query("""
            SELECT rm.reporte.id, COUNT(rm)
            FROM ReporteMiniReporte rm
            WHERE rm.reporte.id IN :idsReportes
            GROUP BY rm.reporte.id
        """)
    List<Object[]> countMiniReportesPorReporte(@Param("idsReportes") List<Integer> idsReportes);



    @Query("""
        SELECT rm.reporte.id, COUNT(DISTINCT rm.miniReporte.usuario.id)
        FROM ReporteMiniReporte rm
        WHERE rm.reporte.id IN :idsReportes
        GROUP BY rm.reporte.id
    """)
    List<Object[]> countUsuariosDistintosPorReporte(@Param("idsReportes") List<Integer> idsReportes);

    @Query("""
        SELECT COUNT(DISTINCT rm.miniReporte.id)
        FROM ReporteMiniReporte rm
        WHERE rm.miniReporte.usuario.id = :usuarioId
        AND rm.reporte.estado.nombre = 'resuelto'
    """)
    long countMiniReportesResueltosPorUsuario(@Param("usuarioId") Integer usuarioId);

    List<ReporteMiniReporte> findByMiniReporteIdIn(List<Integer> idsMiniReportes);
}