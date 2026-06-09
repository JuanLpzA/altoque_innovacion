package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.request.RegistroRequest;
import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.model.Reniec;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.service.ReniecService;
import com.innovacion.altoque.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OcrController {

    private final UsuarioService usuarioService;
    private final ReniecService reniecService;

    @PostMapping("/validar-ocr-servidor")
    public ResponseEntity<ApiResponse<Usuario>> validarOcrServidor(
            @RequestParam("fotoDni") MultipartFile file,
            @RequestParam("dniIngresado") String dniIngresado) {

        try {
            byte[] bytes = file.getBytes();
            BufferedImage imageOriginal = ImageIO.read(new ByteArrayInputStream(bytes));

            if (imageOriginal == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("La imagen capturada está corrupta o vacía."));
            }

            BufferedImage imagenLimpia = optimizarImagenParaOcr(imageOriginal);

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("src/main/resources/tessdata/");
            tesseract.setLanguage("spa");
            tesseract.setVariable("tessedit_char_whitelist",
                    "0123456789PER<ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            tesseract.setPageSegMode(11);

            String textoExtraido = tesseract.doOCR(imagenLimpia)
                    .toUpperCase()
                    .replaceAll("[\\r\\n]+", "\n");


            boolean tieneIndicadorDni =
                    textoExtraido.contains("PERU")        ||
                            textoExtraido.contains("PERÚ")        ||
                            textoExtraido.contains("REPUBLICA")   ||
                            textoExtraido.contains("REPÚBLICA")   ||
                            textoExtraido.contains("IDPER")       ||
                            textoExtraido.replace(" ", "").contains("I<PER") ||
                            textoExtraido.replace(" ", "").contains("IDPER") ||
                            textoExtraido.contains("DNI")         ||
                            textoExtraido.contains("IDENTIDAD")   ||
                            textoExtraido.contains("NACIONAL");

            if (!tieneIndicadorDni) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "No se detectó un DNI peruano válido en la imagen. " +
                                        "Asegúrese de mostrar el DNI físico completo y bien iluminado."));
            }

            String dniDetectado = procesarYExtraerDni(
                    textoExtraido, dniIngresado.trim());

            boolean numeroEncontrado =
                    (dniDetectado != null && dniDetectado.equals(dniIngresado.trim())) ||
                            textoExtraido.replace(" ", "").contains(dniIngresado.trim());

            if (!numeroEncontrado) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "Asegúrese de que sea visible y nítido " +
                                        "dentro del recuadro verde."));
            }

            Usuario usuarioEnBd;
            try {
                usuarioEnBd = usuarioService.buscarPorDni(dniIngresado.trim());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "No se encontró ninguna cuenta asociada al DNI " +
                                        dniIngresado + "."));
            }

            Reniec datosReniec = reniecService.consultarDni(dniIngresado.trim());

            if (datosReniec == null) {
                return ResponseEntity.status(503).body(
                        ApiResponse.error(
                                "No se pudo consultar RENIEC en este momento. " +
                                        "Intente de nuevo en unos segundos."));
            }

            String nombreEnBd     = usuarioEnBd.getNombre()  + " " + usuarioEnBd.getApellido();
            String nombreEnReniec = datosReniec.getNombres() + " " + datosReniec.getApellidos();

            boolean nombreCoincide = sonNombresSimilares(nombreEnBd, nombreEnReniec);

            if (!nombreCoincide) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "Los datos del DNI no coinciden con la cuenta registrada. " +
                                        "Si cree que es un error, contacte al soporte."));
            }

            return ResponseEntity.ok(
                    ApiResponse.ok("Identidad comprobada con éxito.", usuarioEnBd));

        } catch (IOException | TesseractException e) {
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Fallo en el módulo de reconocimiento: " + e.getMessage()));
        }
    }

    @PostMapping("/recuperar-por-dni")
    public ResponseEntity<ApiResponse<String>> recuperarPorDni(
            @RequestBody RegistroRequest req) {
        try {
            if (req.getDni() == null || req.getDni().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("El DNI es obligatorio para la recuperación."));
            }
            usuarioService.recuperarCuentaVulnerada(req);
            return ResponseEntity.ok(
                    ApiResponse.ok("Su cuenta ha sido restablecida con éxito.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al recuperar: " + e.getMessage()));
        }
    }


    private String procesarYExtraerDni(String textoNormalizado, String dniObjetivo) {

        Pattern patternMrzEstricto = Pattern.compile(
                "(?:I|1|0)<PER\\s*(\\d{8})");
        Matcher matcherMrzEstricto = patternMrzEstricto.matcher(
                textoNormalizado.replace(" ", ""));
        if (matcherMrzEstricto.find()) return matcherMrzEstricto.group(1);

        Pattern patternMrzFlechas = Pattern.compile("(\\d{8})<\\d<*");
        Matcher matcherMrzFlechas = patternMrzFlechas.matcher(
                textoNormalizado.replace(" ", ""));
        if (matcherMrzFlechas.find()) return matcherMrzFlechas.group(1);

        String textoCompacto = textoNormalizado.replaceAll("\\s+", "");
        Pattern patternSuelto = Pattern.compile("\\d{8}");
        Matcher matcherSuelto = patternSuelto.matcher(textoCompacto);
        while (matcherSuelto.find()) {
            String candidato = matcherSuelto.group();
            if (candidato.equals(dniObjetivo)) return candidato;
        }

        return null;
    }

    private BufferedImage optimizarImagenParaOcr(BufferedImage src) {
        int nuevoAncho = src.getWidth()  * 2;
        int nuevoAlto  = src.getHeight() * 2;
        BufferedImage imgProcesada = new BufferedImage(
                nuevoAncho, nuevoAlto, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = imgProcesada.createGraphics();
        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, nuevoAncho, nuevoAlto, null);
        g.dispose();
        return imgProcesada;
    }


    private boolean sonNombresSimilares(String nombreBd, String nombreReniec) {
        String a = normalizarNombre(nombreBd);
        String b = normalizarNombre(nombreReniec);

        if (a.equals(b)) return true;

        String[] palabrasA = a.split("\\s+");
        String[] palabrasB = b.split("\\s+");

        long coincidencias = Arrays.stream(palabrasA)
                .filter(p -> p.length() > 2)
                .filter(p -> Arrays.asList(palabrasB).contains(p))
                .count();

        long totalSignificativas = Arrays.stream(palabrasA)
                .filter(p -> p.length() > 2)
                .count();

        if (totalSignificativas == 0) return false;

        double porcentaje = (double) coincidencias / totalSignificativas;
        return porcentaje >= 0.70;
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null) return "";
        return nombre.toUpperCase()
                .trim()
                .replaceAll("[ÁÀÂÄ]", "A")
                .replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[ÍÌÎÏ]", "I")
                .replaceAll("[ÓÒÔÖ]", "O")
                .replaceAll("[ÚÙÛÜ]", "U")
                .replaceAll("[^A-Z0-9 ]", "")
                .replaceAll("\\s+", " ");
    }
}