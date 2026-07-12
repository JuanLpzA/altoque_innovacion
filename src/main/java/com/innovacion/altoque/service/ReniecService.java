package com.innovacion.altoque.service;

import com.innovacion.altoque.model.Reniec;
import com.innovacion.altoque.repository.ReniecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReniecService {

    private final ReniecRepository reniecRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${decolecta.api.url}")
    private String apiUrl;

    @Value("${decolecta.api.token}")
    private String token;

    @Transactional
    public Reniec consultarDni(String dni) {
        return reniecRepository.findByDni(dni)
                .orElseGet(() -> consultarApiYGuardar(dni));
    }

    private Reniec consultarApiYGuardar(String dni) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class,
                    dni
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                System.out.println("====== PROCESANDO MAPEO REAL EN ALTOQUE (consulta nueva a RENIEC) ======");

                String nombres = "";
                if (body.get("first_name") != null) {
                    nombres = String.valueOf(body.get("first_name"));
                } else if (body.get("nombres") != null) {
                    nombres = String.valueOf(body.get("nombres"));
                }

                if (nombres.trim().isEmpty() || "null".equalsIgnoreCase(nombres.trim())) {
                    System.out.println("No se encontró la propiedad 'first_name' en el JSON.");
                    return null;
                }

                String paterno = body.get("first_last_name") != null ? String.valueOf(body.get("first_last_name")) : "";
                String materno = body.get("second_last_name") != null ? String.valueOf(body.get("second_last_name")) : "";
                paterno = "null".equalsIgnoreCase(paterno.trim()) ? "" : paterno.trim();
                materno = "null".equalsIgnoreCase(materno.trim()) ? "" : materno.trim();

                String apellidosUnificados = (paterno + " " + materno).trim();
                if (apellidosUnificados.isEmpty()) {
                    apellidosUnificados = "No Registrado";
                }

                Reniec reniec = new Reniec();
                reniec.setDni(dni);
                reniec.setNombres(nombres.trim());
                reniec.setApellidos(apellidosUnificados);

                System.out.println("Mapeo exitoso -> Nombres: " + reniec.getNombres() + " | Apellidos: " + reniec.getApellidos());

                return reniecRepository.save(reniec);
            }
        } catch (Exception e) {
            System.err.println("Error en ReniecService: " + e.getMessage());
            return null;
        }
        return null;
    }
}