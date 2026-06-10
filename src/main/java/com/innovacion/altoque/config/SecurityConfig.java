package com.innovacion.altoque.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Vistas HTML: libres para que Thymeleaf las sirva sin bloqueo
                        .requestMatchers("/", "/login", "/registro", "/inicio",
                                "/nuevo-reporte", "/mis-reportes", "/reporte-detalle").permitAll()
                        // SecurityConfig:
                        .requestMatchers("/api/categorias", "/api/niveles-riesgo").permitAll()
                        // Recursos estáticos
                        .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                        // Endpoints públicos de la API
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/reportes/cercanos").permitAll()
                        // Solo municipalidad puede gestionar avances y cambiar estados
                        .requestMatchers("/api/avances/**").hasRole("MUNICIPALIDAD")
                        // Todo lo demás de la API requiere token válido
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()

                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}