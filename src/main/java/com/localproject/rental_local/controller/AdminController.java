package com.localproject.rental_local.controller;

import com.localproject.rental_local.dto.AdminDashboardDto;
import com.localproject.rental_local.dto.AdminUserDto;
import com.localproject.rental_local.service.AdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<AdminUserDto> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public AdminUserDto getUserById(@PathVariable Long id) {
        return adminService.getUserById(id);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable Long id) {
        adminService.deactivateUser(id);
    }

    @GetMapping("/dashboard")
    public AdminDashboardDto getDashboardStats() {
        return adminService.getDashboardStats();
    }
}

