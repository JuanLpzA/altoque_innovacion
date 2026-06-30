package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.request.CrearCuentaMunicipalRequest;
import com.innovacion.altoque.dto.response.admin.AdminUsuarioItem;
import com.innovacion.altoque.model.Rol;
import com.innovacion.altoque.model.TokenRecuperacion;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.MiniReporteRepository;
import com.innovacion.altoque.repository.RolRepository;
import com.innovacion.altoque.repository.TokenRecuperacionRepository;
import com.innovacion.altoque.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioAdminService {

    private static final Set<String> ROLES_MUNICIPALES =
            Set.of("municipalidad_admin", "municipalidad_operador");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final MiniReporteRepository miniReporteRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    public List<AdminUsuarioItem> listarCuentasMunicipales() {
        return usuarioRepository.findAll().stream()
                .filter(u -> ROLES_MUNICIPALES.contains(u.getRol().getNombre().toLowerCase()))
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    public List<AdminUsuarioItem> listarCiudadanos() {
        return usuarioRepository.findAll().stream()
                .filter(u -> "ciudadano".equalsIgnoreCase(u.getRol().getNombre()))
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    public AdminUsuarioItem obtenerPorId(Integer id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return toItem(u);
    }

    private AdminUsuarioItem toItem(Usuario u) {
        AdminUsuarioItem item = new AdminUsuarioItem();
        item.setId(u.getId());
        item.setNombre(u.getNombre());
        item.setApellido(u.getApellido());
        item.setDni(u.getDni());
        item.setEmail(u.getEmail());
        item.setTelefono(u.getTelefono());
        item.setRol(u.getRol().getNombre());
        item.setActivo(Boolean.TRUE.equals(u.getActivo()));
        item.setPendienteActivacion(u.getContrasena() == null);
        item.setFechaRegistro(u.getFechaRegistro());
        if ("ciudadano".equalsIgnoreCase(u.getRol().getNombre())) {
            item.setTotalMiniReportes((int) miniReporteRepository.countByUsuarioId(u.getId()));
        }
        return item;
    }


    @Transactional
    public void crearCuentaMunicipal(CrearCuentaMunicipalRequest req) {
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Ya existe una cuenta registrada con ese correo");
        }

        Rol rol = rolRepository.findByNombreIgnoreCase(req.getRol())
                .orElseThrow(() -> new RuntimeException("Rol no válido: " + req.getRol()));

        Usuario u = new Usuario();
        u.setNombre(req.getNombre());
        u.setApellido(req.getApellido());
        u.setEmail(req.getEmail());
        u.setTelefono(req.getTelefono());
        u.setDni(null);
        u.setContrasena(null); // pendiente de activación
        u.setRol(rol);
        u.setActivo(true);
        usuarioRepository.save(u);

        generarYEnviarToken(u, TokenRecuperacion.Tipo.ACTIVACION);
    }


    @Transactional
    public void cambiarActivo(Integer idUsuario, boolean activo) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        u.setActivo(activo);
        usuarioRepository.save(u);
    }


    @Transactional
    public void solicitarReseteoContrasena(Integer idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (u.getEmail() == null) {
            throw new RuntimeException("Esta cuenta no tiene correo registrado, no se puede enviar el enlace");
        }

        generarYEnviarToken(u, TokenRecuperacion.Tipo.RESETEO);
    }

    private void generarYEnviarToken(Usuario u, TokenRecuperacion.Tipo tipo) {
        TokenRecuperacion tr = new TokenRecuperacion();
        tr.setUsuario(u);
        tr.setToken(UUID.randomUUID().toString());
        tr.setTipo(tipo);
        tr.setUsado(false);
        tr.setFechaExpira(LocalDateTime.now().plusHours(24));
        tokenRecuperacionRepository.save(tr);

        if (tipo == TokenRecuperacion.Tipo.ACTIVACION) {
            emailService.enviarCorreoActivacion(u.getEmail(), u.getNombre(), tr.getToken());
        } else {
            emailService.enviarCorreoReseteo(u.getEmail(), u.getNombre(), tr.getToken());
        }
    }

    @Transactional
    public void establecerContrasenaPorToken(String token, String nuevaContrasena) {
        TokenRecuperacion tr = tokenRecuperacionRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Enlace inválido"));

        if (Boolean.TRUE.equals(tr.getUsado())) {
            throw new RuntimeException("Este enlace ya fue utilizado");
        }
        if (tr.getFechaExpira().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Este enlace ha expirado, solicita uno nuevo");
        }

        Usuario u = tr.getUsuario();
        u.setContrasena(passwordEncoder.encode(nuevaContrasena));
        u.setActivo(true);
        usuarioRepository.save(u);

        tr.setUsado(true);
        tokenRecuperacionRepository.save(tr);
    }
}