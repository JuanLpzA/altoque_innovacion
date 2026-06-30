package com.innovacion.altoque.controller;

import com.innovacion.altoque.dto.response.ApiResponse;
import com.innovacion.altoque.dto.response.admin.AdminDashboardStats;
import com.innovacion.altoque.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> stats() {
        return ResponseEntity.ok(ApiResponse.ok("OK", adminService.obtenerStats()));
    }
}