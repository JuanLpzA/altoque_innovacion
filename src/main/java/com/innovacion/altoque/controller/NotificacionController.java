package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.Notificacion;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notificacion>>> misNotificaciones(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                notificacionRepository.findByUsuarioIdAndLeidoFalseOrderByFechaDesc(usuario.getId())));
    }
}