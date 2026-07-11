package com.innovacion.altoque.service;

import com.innovacion.altoque.model.ConfiguracionSistema;
import com.innovacion.altoque.repository.ConfiguracionSistemaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionSistemaRepository repo;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private static final Map<String, String> DEFAULTS = Map.of(
            "limite_reportes_dia", "20",
            "limite_reportes_minuto", "1",
            "radio_agrupacion_metros", "100",
            "dias_vigencia_reporte_abierto", "30"
    );

    @PostConstruct
    public void cargar() {
        repo.findAll().forEach(c -> cache.put(c.getClave(), c.getValor()));
        DEFAULTS.forEach(cache::putIfAbsent);
    }

    public int getInt(String clave) {
        return Integer.parseInt(cache.getOrDefault(clave, DEFAULTS.get(clave)));
    }

    public void actualizar(String clave, String valor) {
        ConfiguracionSistema c = repo.findByClave(clave).orElseGet(() -> {
            ConfiguracionSistema nuevo = new ConfiguracionSistema();
            nuevo.setClave(clave);
            return nuevo;
        });
        c.setValor(valor);
        repo.save(c);
        cache.put(clave, valor);
    }

    public Map<String, String> obtenerTodo() {
        return Map.copyOf(cache);
    }
}