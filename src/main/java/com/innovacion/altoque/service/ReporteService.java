package com.innovacion.altoque.service;

import com.innovacion.altoque.model.*;
import com.innovacion.altoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final NotificacionRepository notificacionRepository;
    private final MiniReporteRepository miniReporteRepository;
    private final ReporteMiniReporteRepository reporteMiniReporteRepository;

    public void notificarCiudadanos(Reporte reporte, String mensaje) {
        List<ReporteMiniReporte> relaciones = reporteMiniReporteRepository
                .findByReporteId(reporte.getId());

        for (ReporteMiniReporte rel : relaciones) {
            Usuario ciudadano = rel.getMiniReporte().getUsuario();
            Notificacion noti = new Notificacion();
            noti.setUsuario(ciudadano);
            noti.setReporte(reporte);
            noti.setMensaje(mensaje);
            notificacionRepository.save(noti);
        }
    }

    public List<Reporte> obtenerTodos() {
        return reporteRepository.findAllByOrderByFechaCreacionDesc();
    }

    public Reporte obtenerPorId(Integer id) {
        return reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
    }
}