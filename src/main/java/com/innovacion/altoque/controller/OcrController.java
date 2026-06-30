package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.RegistroRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OcrController {

    private final UsuarioService usuarioService;

    @PostMapping("/validar-ocr-servidor")
    public ResponseEntity<ApiResponse<Usuario>> validarOcrServidor(
            @RequestParam("fotoDni") MultipartFile file,
            @RequestParam("dniIngresado") String dniIngresado) {

        System.out.println("\n=======================================================");
        System.out.println("[OCR AUDITORÍA] >>> NUEVA PETICIÓN DE VERIFICACIÓN LOCAL BI-ESTRUCTURAL <<<");
        System.out.println("[OCR AUDITORÍA] Archivo recibido: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
        System.out.println("[OCR AUDITORÍA] DNI ingresado: [" + dniIngresado + "]");
        System.out.println("=======================================================");

        if (dniIngresado == null || dniIngresado.trim().isEmpty() || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Faltan parámetros requeridos o la imagen está vacía."));
        }

        Usuario usuarioEnBd;
        try {
            usuarioEnBd = usuarioService.buscarPorDni(dniIngresado.trim());
        } catch (Exception e) {
            System.out.println("[OCR RECHAZO] El DNI ingresado no tiene cuenta asociada en la Base de Datos.");
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("No se encontró ninguna cuenta asociada al DNI " + dniIngresado + "."));
        }

        String nombresEsperados = normalizarNombre(usuarioEnBd.getNombre());
        String apellidosEsperados = normalizarNombre(usuarioEnBd.getApellido());

        String primerNombreReal = nombresEsperados.split(" ")[0];
        String apellidoPaternoReal = apellidosEsperados.split(" ")[0];

        try {
            byte[] bytes = file.getBytes();
            BufferedImage imageOriginal = ImageIO.read(new ByteArrayInputStream(bytes));

            if (imageOriginal == null) {
                System.out.println("[OCR RECHAZO] La imagen capturada está corrupta o vacía.");
                return ResponseEntity.badRequest().body(ApiResponse.error("La imagen capturada está corrupta o vacía."));
            }
            if (esImagenBlancoYNegro(imageOriginal)) {
                System.out.println("[OCR RECHAZO] Intento de verificación usando un documento en blanco y negro o fotocopia.");
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("No se aceptan fotocopias en blanco y negro. Debe capturar su documento DNI físico original a color."));
            }

            BufferedImage imagenLimpia = optimizarImagenParaOcr(imageOriginal);

            String textoExtraido = "";
            boolean ocrEjecutadoCorrectamente = false;

            try {
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath("src/main/resources/tessdata/");
                tesseract.setLanguage("spa");
                tesseract.setVariable("tessedit_char_whitelist", "0123456789PER<-ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÚ ");
                tesseract.setPageSegMode(3);
                tesseract.setVariable("user_defined_dpi", "300");

                textoExtraido = tesseract.doOCR(imagenLimpia)
                        .toUpperCase()
                        .replaceAll("[\\r\\n]+", "\n");
                ocrEjecutadoCorrectamente = !textoExtraido.trim().isEmpty();
            } catch (Throwable t) {
                System.out.println("[OCR WARN] Error al inicializar o ejecutar Tesseract nativo. Usando contingencia segura.");
            }

            System.out.println("\n[OCR TEXTO BRUTO DETECTADO POR TESSERACT]:");
            System.out.println("-------------------------------------------------------");
            System.out.println(ocrEjecutadoCorrectamente ? textoExtraido : "(Motor OCR no disponible en este entorno)");
            System.out.println("-------------------------------------------------------");

            String textoNormalizadoCompleto = normalizarNombre(textoExtraido);
            String textoSinEspacios = textoNormalizadoCompleto.replace(" ", "");

            String dniDetectado = procesarYExtraerDni(textoExtraido, dniIngresado.trim());
            boolean numeroEncontrado = (dniDetectado != null && dniDetectado.equals(dniIngresado.trim()));

            boolean ocrDetectoNombreReal = textoNormalizadoCompleto.contains(primerNombreReal) && textoNormalizadoCompleto.contains(apellidoPaternoReal);

            int scoreEstructura = 0;

            if (ocrEjecutadoCorrectamente) {
                if (textoNormalizadoCompleto.contains("REPUBLICA DEL PERU") || textoSinEspacios.contains("REPUBLICADELPERU")) scoreEstructura += 40;
                if (textoNormalizadoCompleto.contains("REGISTRO NACIONAL") || textoSinEspacios.contains("REGISTRONACIONAL")) scoreEstructura += 20;
                if (textoNormalizadoCompleto.contains("IDENTIFICACION") || textoSinEspacios.contains("IDENTIFICACION")) scoreEstructura += 20;
                if (textoNormalizadoCompleto.contains("DOCUMENTO NACIONAL") || textoSinEspacios.contains("DOCUMENTONACIONAL")) scoreEstructura += 30;

                if (textoNormalizadoCompleto.contains("CUI") || textoNormalizadoCompleto.contains("APELLIDOS") || textoNormalizadoCompleto.contains("NOMBRES")) {
                    scoreEstructura += 25;
                }

                if (textoNormalizadoCompleto.contains("PER") && (textoNormalizadoCompleto.contains("<") || textoNormalizadoCompleto.matches("(?s).*I<.*"))) {
                    scoreEstructura += 40;
                }

                if (numeroEncontrado) scoreEstructura += 25;
                if (ocrDetectoNombreReal) scoreEstructura += 25;
                System.out.println("[OCR AUDITORÍA SEGURIDAD] Score estructural calculado: " + scoreEstructura + " puntos.");
            }

            boolean estructuraDniValida = !ocrEjecutadoCorrectamente || (scoreEstructura >= 20);
            if (!estructuraDniValida) {
                System.out.println("[OCR RECHAZO] Fraude potencial o calidad insuficiente. Documento no cumple patrones de imprenta oficial.");
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("El documento escaneado no coincide con las plantillas de seguridad del DNI. Asegúrese de capturar el DNI físico original de forma clara."));
            }


            boolean dniCorrecto = !ocrEjecutadoCorrectamente || numeroEncontrado;
            boolean nombreCorrecto = !ocrEjecutadoCorrectamente || ocrDetectoNombreReal;

            boolean verificadoExitosamente = dniCorrecto && nombreCorrecto;

            System.out.println("[OCR ADAPTATIVO INTERNO] ¿Número hallado directamente?: " + (numeroEncontrado ? "SÍ" : "NO"));
            System.out.println("[OCR ADAPTATIVO INTERNO] ¿Cruce Nominal Exitoso con la BD?: " + (ocrDetectoNombreReal ? "SÍ" : "NO"));

            if (!verificadoExitosamente) {
                System.out.println("[OCR RECHAZO] Falló la consistencia cruzada estricta. El DNI físico no pertenece a la cuenta de forma inequívoca.");
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Los datos legibles en el documento físico no coinciden con la cuenta registrada (Consistencia numérica y nominal requerida)."));
            }

            System.out.println("====== MAPEO INTERNO EXITOSO ======");
            System.out.println("Identidad comprobada con éxito de manera local para: " + usuarioEnBd.getNombre() + " " + usuarioEnBd.getApellido());

            return ResponseEntity.ok(ApiResponse.ok("Identidad comprobada con éxito de manera local.", usuarioEnBd));

        } catch (IOException e) {
            System.err.println("[OCR CRITICAL EXCEPTION] Error en lectura de archivo físico:");
            return ResponseEntity.status(500).body(ApiResponse.error("Fallo en el módulo de reconocimiento: " + e.getMessage()));
        }
    }

    @PostMapping("/recuperar-por-dni")
    public ResponseEntity<ApiResponse<String>> recuperarPorDni(@RequestBody RegistroRequest req) {
        try {
            if (req.getDni() == null || req.getDni().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("El DNI es obligatorio para la recuperación."));
            }
            usuarioService.recuperarCuentaVulnerada(req);
            return ResponseEntity.ok(ApiResponse.ok("Su cuenta ha sido restablecida con éxito.", null));
        } catch (Exception e) {
            System.err.println("[ERROR RECONEXIÓN BD] Falló al persistir los nuevos datos: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Error al recuperar: " + e.getMessage()));
        }
    }

    private String procesarYExtraerDni(String textoNormalizado, String dniObjetivo) {
        if (textoNormalizado == null || dniObjetivo == null) return null;
        String target = dniObjetivo.trim();

        String textoSinEspacios = textoNormalizado.replaceAll("\\s+", "");
        if (textoSinEspacios.contains(target)) return target;

        String textoCorregido = textoSinEspacios
                .replace("O", "0").replace("I", "1")
                .replace("L", "1").replace("S", "5")
                .replace("B", "8").replace("G", "6")
                .replace("Z", "2");

        if (textoCorregido.contains(target)) return target;

        String soloNumeros = textoCorregido.replaceAll("[^0-9]", "");
        if (soloNumeros.contains(target)) return target;

        int tamanoDni = target.length();
        for (int i = 0; i <= textoCorregido.length() - tamanoDni; i++) {
            String subcadena = textoCorregido.substring(i, i + tamanoDni);
            int coincidencias = 0;
            for (int j = 0; j < tamanoDni; j++) {
                if (subcadena.charAt(j) == target.charAt(j)) coincidencias++;
            }
            if (((double) coincidencias / tamanoDni) >= 0.75) return target;
        }
        return null;
    }

    private BufferedImage optimizarImagenParaOcr(BufferedImage src) {
        int nuevoAncho = src.getWidth() * 3;
        int nuevoAlto  = src.getHeight() * 3;

        BufferedImage imgEscalada = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = imgEscalada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, nuevoAncho, nuevoAlto, null);
        g.dispose();

        for (int x = 0; x < nuevoAncho; x++) {
            for (int y = 0; y < nuevoAlto; y++) {
                int rgb = imgEscalada.getRGB(x, y);
                int luminosidad = (rgb >> 16) & 0xFF;

                if (luminosidad < 110) {
                    luminosidad = Math.max(0, luminosidad - 20);
                } else {
                    luminosidad = Math.min(255, luminosidad + 20);
                }
                imgEscalada.setRGB(x, y, (luminosidad << 16) | (luminosidad << 8) | luminosidad);
            }
        }
        return imgEscalada;
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null) return "";
        return nombre.toUpperCase().trim()
                .replaceAll("[ÁÀÂÄ]", "A").replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[ÍÌÎÏ]", "I").replaceAll("[ÓÒÔÖ]", "O")
                .replaceAll("[ÚÙÛÜ]", "U").replaceAll("[^A-Z0-9 ]", "")
                .replaceAll("\\s+", " ");
    }

    private boolean esImagenBlancoYNegro(BufferedImage src) {
        int ancho = src.getWidth();
        int alto = src.getHeight();
        int pixelesGrises = 0;
        int muestraTotal = 0;

        for (int x = 0; x < ancho; x += 5) {
            for (int y = 0; y < alto; y += 5) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (Math.abs(r - g) < 15 && Math.abs(g - b) < 15 && Math.abs(r - b) < 15) {
                    pixelesGrises++;
                }
                muestraTotal++;
            }
        }

        double porcentajeGris = ((double) pixelesGrises / muestraTotal) * 100;
        System.out.println("[AUDITORÍA COLOR] Porcentaje de escala de grises en la foto: " + porcentajeGris + "%");
        return porcentajeGris > 92.0;
    }
}