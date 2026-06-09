package com.innovacion.altoque.repository;

import com.innovacion.altoque.model.AvanceReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvanceReporteRepository extends JpaRepository<AvanceReporte, Integer> {
    List<AvanceReporte> findByReporteIdOrderByFechaDesc(Integer reporteId);
}