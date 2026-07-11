package com.innovacion.altoque.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void enviarCorreoActivacion(String destinatario, String nombre, String token) {
        String link = baseUrl + "/establecer-contrasena?token=" + token;
        String asunto = "Activa tu cuenta en Altoque – Panel Municipal";
        String html = """
                <p>Hola %s,</p>
                <p>Se ha creado una cuenta para ti en Altoque, el sistema de reportes de incidencias
                municipales, para que accedas al panel administrativo de la municipalidad.</p>
                <p>Para activar tu cuenta y establecer tu contraseña por primera vez, ingresa al
                siguiente enlace (válido por 24 horas):</p>
                <p><a href="%s">%s</a></p>
                <p>Si no esperabas este correo, puedes ignorarlo.</p>
                <p>— Equipo Altoque</p>
                """.formatted(nombre, link, link);
        enviar(destinatario, asunto, html);
    }

    public void enviarCorreoReseteo(String destinatario, String nombre, String token) {
        String link = baseUrl + "/establecer-contrasena?token=" + token;
        String asunto = "Restablece tu contraseña – Altoque";
        String html = """
                <p>Hola %s,</p>
                <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en el panel
                municipal de Altoque.</p>
                <p>Para definir una nueva contraseña, ingresa al siguiente enlace (válido por 24 horas):</p>
                <p><a href="%s">%s</a></p>
                <p>Si tú no solicitaste este cambio, puedes ignorar este correo.</p>
                <p>— Equipo Altoque</p>
                """.formatted(nombre, link, link);
        enviar(destinatario, asunto, html);
    }

    private void enviar(String destinatario, String asunto, String htmlContent) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", senderName, "email", senderEmail));
            body.put("to", List.of(Map.of("email", destinatario)));
            body.put("subject", asunto);
            body.put("htmlContent", htmlContent);

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                throw new RuntimeException("Error enviando correo: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar el correo a " + destinatario, e);
        }
    }
}