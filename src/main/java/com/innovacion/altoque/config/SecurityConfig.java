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
                        .requestMatchers("/", "/login", "/registro", "/inicio",
                                "/nuevo-reporte", "/mis-reportes", "/reporte-detalle",
                                "/recuperar-cuenta", "/establecer-contrasena").permitAll()
                        .requestMatchers("/admin/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/categorias", "/api/niveles-riesgo").permitAll()
                        .requestMatchers("/api/reportes/cercanos", "/api/reportes/top-cercanos").permitAll()
                        .requestMatchers("/api/geo/reverse").permitAll()
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .requestMatchers("/api/admin/password/**").permitAll() // establecer/resetear contraseña vía token
                        // Gestión de usuarios (crear cuentas, activar/desactivar, resetear) solo para admin
                        .requestMatchers("/api/admin/usuarios/**").hasRole("MUNICIPALIDAD_ADMIN")
                        // Resto del panel municipal: admin u operador
                        .requestMatchers("/api/admin/configuracion/**").hasRole("MUNICIPALIDAD_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("MUNICIPALIDAD_ADMIN", "MUNICIPALIDAD_OPERADOR")
                        .requestMatchers("/api/avances/**").hasAnyRole("MUNICIPALIDAD_ADMIN", "MUNICIPALIDAD_OPERADOR")
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