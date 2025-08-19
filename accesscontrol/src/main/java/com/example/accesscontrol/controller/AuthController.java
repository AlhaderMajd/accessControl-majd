package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.LoginAuthResponse;
import com.example.accesscontrol.dto.auth.RegisterAuthResponse;
import com.example.accesscontrol.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with email & password")
    @PostMapping("/login")
    public ResponseEntity<LoginAuthResponse> login(@Valid @RequestBody AuthRequest request) {
        LoginAuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register with email & password")
    @PostMapping("/register")
    public ResponseEntity<RegisterAuthResponse> register(@Valid @RequestBody AuthRequest request) {
        RegisterAuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

