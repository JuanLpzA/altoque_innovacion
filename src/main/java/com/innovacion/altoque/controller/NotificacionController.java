package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.NotificacionResponse;
import com.innovacion.altoque.model.Notificacion;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificacionResponse>>> misNotificaciones(
            @AuthenticationPrincipal Usuario usuario) {
        List<Notificacion> lista = notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId());
        List<NotificacionResponse> dto = lista.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("OK", dto));
    }

    @GetMapping("/no-leidas-count")
    public ResponseEntity<ApiResponse<Long>> contarNoLeidas(@AuthenticationPrincipal Usuario usuario) {
        long count = notificacionRepository.countByUsuarioIdAndLeidoFalse(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok("OK", count));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<ApiResponse<String>> marcarLeida(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuario) {
        Notificacion n = notificacionRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
        n.setLeido(true);
        notificacionRepository.save(n);
        return ResponseEntity.ok(ApiResponse.ok("Marcada como leída", null));
    }

    @PatchMapping("/leer-todas")
    @Transactional
    public ResponseEntity<ApiResponse<String>> marcarTodasLeidas(@AuthenticationPrincipal Usuario usuario) {
        notificacionRepository.marcarTodasComoLeidas(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok("Todas marcadas como leídas", null));
    }

    private NotificacionResponse toResponse(Notificacion n) {
        NotificacionResponse r = new NotificacionResponse();
        r.setId(n.getId());
        r.setMensaje(n.getMensaje());
        r.setLeido(n.getLeido());
        r.setFecha(n.getFecha());
        if (n.getReporte() != null) {
            r.setReporteId(n.getReporte().getId());
            r.setReporteTitulo(n.getReporte().getTitulo());
        }
        return r;
    }
}