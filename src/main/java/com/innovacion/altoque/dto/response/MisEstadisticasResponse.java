package com.innovacion.altoque.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MisEstadisticasResponse {
    private long totalEnviados;
    private long resueltos;
}