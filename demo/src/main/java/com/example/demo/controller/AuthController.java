package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.dto.request.GoogleLoginRequest;
import com.example.demo.dto.response.GoogleLoginResponse;
import com.example.demo.entity.User;
import com.example.demo.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<GoogleLoginResponse> googleLogin(
            @RequestBody GoogleLoginRequest request) {
        GoogleLoginResponse response = authService.googleLogin(request.getToken());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/username")
    public ResponseEntity<?> updateUserName(@PathVariable UUID userId, @RequestBody Map<String, String> body) {
        String userName = body.get("userName");
        if (userName == null || userName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ユーザー名は必須です"));
        }
        authService.updateUserName(userId, userName);
        return ResponseEntity.ok(Map.of("message", "ユーザー名を更新しました"));
    }
}