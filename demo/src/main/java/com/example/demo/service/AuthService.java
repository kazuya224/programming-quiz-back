package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.example.demo.dto.SignupRequest; // 追加
import com.example.demo.dto.response.GoogleLoginResponse;
import com.example.demo.service.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${google.client-id}")
    private String clientId;

    public GoogleLoginResponse googleLogin(String token) {
        GoogleIdToken.Payload payload = verifyToken(token);

        String googleId = payload.getSubject();
        String email = payload.getEmail();

        Optional<User> userOpt = userRepository.findByGoogleId(googleId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String jwt = jwtService.generateToken(user.getUserId());

            return new GoogleLoginResponse(
                    jwt,
                    user.getUserName() == null,
                    user.getUserName());
        }

        // 新規登録
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUserName("Engineer");
        newUser.setGoogleId(googleId);

        userRepository.save(newUser);
        String jwt = jwtService.generateToken(newUser.getUserId());

        return new GoogleLoginResponse(
                jwt,
                true,
                newUser.getUserName());
    }

    private GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new JacksonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new RuntimeException("Invalid token");
            }

            return idToken.getPayload();

        } catch (Exception e) {
            e.printStackTrace(); // ←重要
            throw new RuntimeException("Token verification failed", e);
        }
    }

    public void updateUserName(UUID userId, String userName) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        user.setUserName(userName);
        userRepository.save(user);
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}