package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.NivelRiesgo;
import com.innovacion.altoque.repository.NivelRiesgoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/niveles-riesgo")
@RequiredArgsConstructor
public class NivelRiesgoController {

    private final NivelRiesgoRepository nivelRiesgoRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NivelRiesgo>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("OK", nivelRiesgoRepository.findAll()));
    }
}