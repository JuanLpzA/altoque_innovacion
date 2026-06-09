package com.innovacion.altoque.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VistasController {

    @GetMapping({"/", "/login"})
    public String login() { return "login"; }

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
}