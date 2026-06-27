package com.innovacion.altoque.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VistasController {

    private boolean esMobil(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return false;
        ua = ua.toLowerCase();
        return ua.contains("android") || ua.contains("iphone") ||
                ua.contains("ipad") || ua.contains("mobile") ||
                ua.contains("opera mini") || ua.contains("blackberry");
    }

    @GetMapping({"/", "/login"})
    public String login(HttpServletRequest request) {
        return esMobil(request) ? "login" : "login-admin";
    }

    // Vistas ciudadano
    @GetMapping("/registro")
    public String registro() { return "registro"; }

    @GetMapping("/inicio")
    public String inicio() { return "inicio"; }

    @GetMapping("/nuevo-reporte")
    public String nuevoReporte() { return "nuevo-reporte"; }

    @GetMapping("/mis-reportes")
    public String misReportes() { return "mis-reportes"; }

    @GetMapping("/recuperar-cuenta")
    public String recuperarCuenta() { return "recuperar-cuenta"; }

    @GetMapping("/reporte-detalle")
    public String reporteDetalle() { return "reporte-detalle"; }

    // Vistas admin
    @GetMapping("/admin/login")
    public String adminLogin() { return "login-admin"; }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() { return "admin/dashboard"; }

    @GetMapping("/admin/reportes")
    public String adminReportes() { return "admin/reportes"; }

    @GetMapping("/admin/reporte-detalle")
    public String adminReporteDetalle() { return "admin/reporte-detalle"; }

    @GetMapping("/admin/moderacion")
    public String adminModeracion() { return "admin/moderacion"; }

    @GetMapping("/admin/mapa")
    public String adminMapa() { return "admin/mapa"; }

    @GetMapping("/admin/evidencias")
    public String adminEvidencias() { return "admin/evidencias"; }
}