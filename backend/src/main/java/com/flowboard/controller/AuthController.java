package com.flowboard.controller;

import com.flowboard.dto.AuthResponse;
import com.flowboard.dto.LoginRequest;
import com.flowboard.dto.RegisterRequest;
import com.flowboard.dto.UpdateUserProfileRequest;
import com.flowboard.dto.UserDTO;
import com.flowboard.service.AuthService;
import com.flowboard.service.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private final AuthService authService;
    private final JWTService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        return ResponseEntity.ok(authService.getUserProfile(userId.toString()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody UpdateUserProfileRequest request) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        return ResponseEntity.ok(authService.updateUserProfile(userId.toString(), request));
    }
}
