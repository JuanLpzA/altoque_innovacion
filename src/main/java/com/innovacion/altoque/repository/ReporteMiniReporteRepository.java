package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.ReporteMiniReporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteMiniReporteRepository extends JpaRepository<ReporteMiniReporte, Integer> {
    List<ReporteMiniReporte> findByReporteId(Integer reporteId);

}