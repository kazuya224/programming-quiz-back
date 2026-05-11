package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.dto.request.GoogleLoginRequest;
import com.example.demo.dto.response.GoogleLoginResponse;
import com.example.demo.dto.response.MeResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserProgressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;
import com.example.demo.service.JwtService;
import com.example.demo.service.CookieService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = { "http://localhost:3000",
        "https://programing-quiz-zeta.vercel.app",
        "https://app.devtrain-app.com" }, allowCredentials = "true")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;
    private final UserProgressRepository userProgressRepository;

    @PostMapping("/google")
    public ResponseEntity<Void> googleLogin(
            @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {

        String token = authService.googleLogin(request.getToken()).getToken();

        System.out.println("before add cookie");
        cookieService.addTokenCookie(response, token);
        System.out.println("after add cookie");

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/username")
    public ResponseEntity<Void> updateUserName(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String userName = body.get("userName");

        authService.updateUserName(userId, userName);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        User user = authService.findById(userId);
        boolean hasAnswered = userProgressRepository.existsByUserId(userId);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(new MeResponse(user.getUserName(), hasAnswered));
    }

}