package com.innovacion.altoque.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovacion.altoque.dto.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private RestTemplate crearRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(6000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    @GetMapping("/reverse")
    public ResponseEntity<ApiResponse<String>> reverse(@RequestParam double lat, @RequestParam double lng) {
        String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat + "&lon=" + lng
                + "&format=json&zoom=17&addressdetails=1";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "AltoqueApp/1.0 (contacto@municipalidad-chiclayo.gob.pe)");
            headers.set("Accept-Language", "es");

            ResponseEntity<String> resp = crearRestTemplate()
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            String rawBody = resp.getBody();
            System.out.println("[geo] status: " + resp.getStatusCode() + " | body: " + rawBody);

            if (rawBody == null || rawBody.isBlank()) {
                return ResponseEntity.ok(ApiResponse.error("Sin resultado"));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode body = mapper.readTree(rawBody);

            if (body.has("error")) {
                System.err.println("[geo] Nominatim devolvió error: " + body.path("error").asText());
                return ResponseEntity.ok(ApiResponse.error("Sin resultado"));
            }

            JsonNode address = body.path("address");
            String aprox = Stream.of("road", "suburb", "city_district", "neighbourhood")
                    .map(k -> address.path(k).asText(null))
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(2)
                    .collect(Collectors.joining(", "));
            if (aprox.isBlank()) aprox = body.path("display_name").asText("");
            if (aprox.isBlank()) return ResponseEntity.ok(ApiResponse.error("Sin resultado"));

            return ResponseEntity.ok(ApiResponse.ok("OK", aprox));
        } catch (Exception e) {
            System.err.println("[geo] Error en reverse geocoding [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("No disponible"));
        }
    }
}