package com.innovacion.altoque.service;

import com.innovacion.altoque.model.Reniec;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import java.util.Map;

@Service
public class ReniecService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "https://api.decolecta.com/v1/reniec/dni?numero={dni}";
    private final String TOKEN = "sk_16813.7PQ2ZVJTG1d3nS7DiFBPYiwJDbSje6GC";

    public Reniec consultarDni(String dni) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + TOKEN);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.GET,
                    entity,
                    Map.class,
                    dni
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                System.out.println("====== PROCESANDO MAPEO REAL EN ALTOQUE ======");

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
                return reniec;
            }
        } catch (Exception e) {
            System.err.println("Error en ReniecService: " + e.getMessage());
            return null;
        }
        return null;
    }
}