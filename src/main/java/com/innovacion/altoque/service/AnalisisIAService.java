package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.response.AnalisisIAResponse;
import com.innovacion.altoque.model.Categoria;
import com.innovacion.altoque.repository.CategoriaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalisisIAService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final CategoriaRepository categoriaRepository;

    private static final String URL_GEMINI =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=";

    public AnalisisIAResponse analizarFoto(String urlFotoCloudinary) {
        AnalisisIAResponse fallback = buildFallback();

        List<Categoria> categorias = categoriaRepository.findAll();
        String listaCategorias = categorias.stream()
                .map(c -> "ID " + c.getId() + ": " + c.getNombre())
                .collect(Collectors.joining("\n"));

        try {
            System.out.println("Descargando imagen desde Cloudinary: " + urlFotoCloudinary);
            byte[] imageBytes = descargarImagen(urlFotoCloudinary);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            System.out.println("Imagen descargada: " + imageBytes.length + " bytes");

            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            String systemInstruction =
                    """
                    Eres un analizador de incidencias municipales para Lambayeque, Perú.
            
                    Tu tarea es analizar UNA imagen enviada por un ciudadano y determinar
                    si realmente muestra una incidencia urbana visible.
            
                    IMPORTANTE:
                    - NO inventes problemas.
                    - NO asumas cosas que no se ven claramente.
                    - Si tienes dudas, responde valido=false.
                    - Sé conservador al validar imágenes.
            
                    CATEGORÍAS DISPONIBLES:
                    """ + listaCategorias + """

        DEFINICIONES DE CATEGORÍAS:
        - Limpieza pública: Basura acumulada, desmonte, residuos sólidos, suciedad en calles.
        - Seguridad ciudadana: Peleas, robos, violencia, vandalismo, situaciones peligrosas visibles.
        - Infraestructura vial: Baches, pistas dañadas, veredas rotas, señales destruidas.
        - Alumbrado público: Postes apagados, luminarias dañadas, cables expuestos.
        - Áreas verdes: Parques abandonados, árboles caídos, jardines deteriorados.
        - Tránsito vehicular: Accidentes, congestión severa, obstrucciones viales.
        - Contaminación ambiental: Humo excesivo, aguas contaminadas, quema de basura.

        RECHAZA con valido=false si:
        - Es selfie o foto de persona posando
        - Es interior de casa
        - No hay problema municipal visible
        - Está borrosa u oscura
        - Es meme, captura de pantalla o imagen irrelevante

        NIVELES DE RIESGO:
        - ALTO: Riesgo inmediato (cables expuestos, incendio, violencia, colapso)
        - MEDIO: Problema importante no crítico (baches, basura, alumbrado apagado)
        - BAJO: Problema menor o estético (grass descuidado, grafiti pequeño)

        Responde SOLO JSON válido.
        """;

            String userContent =
                    """
                    Analiza la imagen enviada por el ciudadano.
            
                    Si NO es una incidencia municipal clara:
                    {"valido":false,"titulo":"","descripcion":"","idCategoria":0,"categoriaDetectada":"","nivelRiesgo":"BAJO","confianza":0}
            
                    Si SÍ es válida:
                    {"valido":true,"titulo":"texto","descripcion":"texto","idCategoria":1,"categoriaDetectada":"texto","nivelRiesgo":"BAJO|MEDIO|ALTO","confianza":85.5}
            
                    REGLAS: título corto, descripción solo de lo visible, no inventes detalles.
                    """;

            Map<String, Object> requestBody = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", systemInstruction))
                    ),
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", userContent),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", "image/jpeg",
                                            "data", base64Image
                                    ))
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "response_mime_type", "application/json"
                    )
            );

            // ── 4. Llamar a Gemini ────────────────────────────────────
            System.out.println("Enviando imagen a Gemini...");
            String respuestaRaw = restTemplate.postForObject(
                    URL_GEMINI + apiKey, requestBody, String.class
            );

            if (respuestaRaw == null) {
                System.err.println("Gemini devolvió respuesta null");
                return fallback;
            }

            // ── 5. Parsear respuesta ──────────────────────────────────
            JsonNode root = mapper.readTree(respuestaRaw);
            String textoIA = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim();

            System.out.println("Respuesta Gemini: " + textoIA);

            if (textoIA.contains("{")) {
                textoIA = textoIA.substring(textoIA.indexOf("{"), textoIA.lastIndexOf("}") + 1);
            }

            JsonNode datos = mapper.readTree(textoIA);

            if (!datos.path("valido").asBoolean(false)) {
                System.out.println("Gemini: imagen no válida como incidencia");
                return fallback;
            }

            int idCategoria = datos.path("idCategoria").asInt(0);
            double confianza = datos.path("confianza").asDouble(0);

            boolean categoriaExiste = categorias.stream()
                    .anyMatch(c -> c.getId().equals(idCategoria));

            if (!categoriaExiste) {
                System.err.println("Categoría inválida devuelta por Gemini: " + idCategoria);
                return fallback;
            }

            if (confianza < 70) {
                System.out.println("Confianza baja (" + confianza + "%) — activando modo manual");
                return fallback;
            }

            AnalisisIAResponse resp = new AnalisisIAResponse();
            resp.setTitulo(datos.path("titulo").asText(""));
            resp.setDescripcion(datos.path("descripcion").asText(""));
            resp.setIdCategoria(idCategoria);
            resp.setCategoriaDetectada(datos.path("categoriaDetectada").asText(""));
            resp.setNivelRiesgo(datos.path("nivelRiesgo").asText("BAJO").toUpperCase());
            resp.setConfianza(confianza);
            resp.setFallback(false);

            System.out.println("Análisis exitoso: \"" + resp.getTitulo()
                    + "\" | Categoría ID: " + idCategoria
                    + " | Riesgo: " + resp.getNivelRiesgo()
                    + " | Confianza: " + confianza + "%");
            return resp;

        } catch (Exception e) {
            System.err.println("Error en AnalisisIA [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
            return fallback;
        }
    }

    private byte[] descargarImagen(String url) throws Exception {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        RestTemplate rt = new RestTemplate(factory);
        byte[] bytes = rt.getForObject(url, byte[].class);

        if (bytes == null || bytes.length == 0)
            throw new Exception("Imagen vacía al descargar desde Cloudinary: " + url);

        return bytes;
    }

    private AnalisisIAResponse buildFallback() {
        AnalisisIAResponse r = new AnalisisIAResponse();
        r.setTitulo("");
        r.setDescripcion("");
        r.setIdCategoria(0);
        r.setCategoriaDetectada("");
        r.setNivelRiesgo("BAJO");
        r.setConfianza(0);
        r.setFallback(true);
        return r;
    }
}