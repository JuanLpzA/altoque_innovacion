package com.innovacion.altoque.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void enviarCorreoActivacion(String destinatario, String nombre, String token) {
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
    }

    public void enviarCorreoReseteo(String destinatario, String nombre, String token) {
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
    }

    private void enviar(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.send(mensaje);
    }
}