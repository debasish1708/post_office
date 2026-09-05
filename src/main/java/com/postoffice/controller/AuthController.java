package com.postoffice.controller;

import com.postoffice.dto.AuthRequest;
import com.postoffice.dto.UserDto;
import com.postoffice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse) {
        try {
            return ResponseEntity.ok(authService.register(request, httpRequest, httpResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        try {
            if (request.getEmail() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
            }
            UserDto user = authService.login(request.getEmail(), request.getPassword(), httpRequest, httpResponse);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        try {
            return ResponseEntity.ok(UserDto.from(authService.requireCurrentUser()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
    }
}
