package com.innovacion.altoque.config;

import com.innovacion.altoque.model.Usuario;
import com.innovacion.altoque.repository.UsuarioRepository;
import com.innovacion.altoque.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.esValido(token)) {
                String dni = jwtUtil.extraerDni(token);
                String rol  = jwtUtil.extraerRol(token);

                Usuario usuario = usuarioRepository.findByDni(dni).orElse(null);
                if (usuario != null && usuario.getActivo()) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            usuario, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }
}