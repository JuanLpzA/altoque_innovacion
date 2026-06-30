package com.innovacion.altoque.service;

import com.innovacion.altoque.dto.request.RegistroRequest;
import com.innovacion.altoque.model.Rol;
import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.RolRepository;
import com.innovacion.altoque.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario registrar(RegistroRequest req) {
        if (usuarioRepository.existsByDni(req.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }
        Rol rolCiudadano = rolRepository.findByNombreIgnoreCase("ciudadano")
                .orElseThrow(() -> new RuntimeException("Rol ciudadano no encontrado"));

        Usuario u = new Usuario();
        u.setNombre(req.getNombre());
        u.setApellido(req.getApellido());
        u.setDni(req.getDni());
        u.setTelefono(req.getTelefono());
        u.setContrasena(passwordEncoder.encode(req.getContrasena()));
        u.setRol(rolCiudadano);
        return usuarioRepository.save(u);
    }

    public Usuario buscarPorDni(String dni) {
        return usuarioRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Transactional
    public void recuperarCuentaVulnerada(RegistroRequest req) {
        Usuario usuarioExistente = usuarioRepository.findByDni(req.getDni())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioExistente.setTelefono(req.getTelefono());
        usuarioExistente.setContrasena(passwordEncoder.encode(req.getContrasena()));

        usuarioExistente.setActivo(true);

        usuarioRepository.save(usuarioExistente);
    }
    public boolean existePorDni(String dni) {
        return usuarioRepository.existsByDni(dni);
    }
}