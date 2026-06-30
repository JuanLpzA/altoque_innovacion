package com.innovacion.altoque.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void enviarCorreoActivacion(String destinatario, String nombre, String token) {
        try {
            String link = baseUrl + "/establecer-contrasena?token=" + token;
            String asunto = "Activa tu cuenta en Altoque – Panel Municipal";
            String cuerpo = """
                    Hola %s,
                    Se ha creado una cuenta para ti en Altoque, el sistema de reportes de incidencias
                    municipales, para que accedas al panel administrativo de la municipalidad.
                    Para activar tu cuenta y establecer tu contraseña por primera vez, ingresa al
                    siguiente enlace (válido por 24 horas):
                    %s
                    Si no esperabas este correo, puedes ignorarlo.
                    — Equipo Altoque
                    """.formatted(nombre, link);
            enviar(destinatario, asunto, cuerpo);
        } catch (Exception e) {
            System.err.println("Error enviando correo de activación a " + destinatario + ": " + e.getMessage());
        }
    }

    @Async
    public void enviarCorreoReseteo(String destinatario, String nombre, String token) {
        try {
            String link = baseUrl + "/establecer-contrasena?token=" + token;
            String asunto = "Restablece tu contraseña – Altoque";
            String cuerpo = """
                    Hola %s,
                    Recibimos una solicitud para restablecer la contraseña de tu cuenta en el panel
                    municipal de Altoque, el sistema de reportes de incidencias municipales.
                    Para definir una nueva contraseña, ingresa al siguiente enlace (válido por 24 horas):
                    %s
                    Si tú no solicitaste este cambio, puedes ignorar este correo; tu contraseña actual
                    seguirá funcionando con normalidad.
                    — Equipo Altoque
                    """.formatted(nombre, link);
            enviar(destinatario, asunto, cuerpo);
        } catch (Exception e) {
            System.err.println("Error enviando correo de reseteo a " + destinatario + ": " + e.getMessage());
        }
    }

    private void enviar(String destinatario, String asunto, String cuerpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", fromEmail);
        body.put("to", new String[]{destinatario});
        body.put("subject", asunto);
        body.put("text", cuerpo);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
    }
}