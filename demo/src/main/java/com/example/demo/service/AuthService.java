package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.example.demo.dto.SignupRequest; // 追加
import com.example.demo.dto.response.GoogleLoginResponse;

import lombok.RequiredArgsConstructor;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public GoogleLoginResponse googleLogin(String token) {
        GoogleIdToken.Payload payload = verifyToken(token);

        String googleId = payload.getSubject();
        String email = payload.getEmail();

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                userRepository.save(user);
            }

            return new GoogleLoginResponse(
                    user.getUserId(),
                    user.getUserName() == null);
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUserName(null);
        newUser.setGoogleId(googleId);

        userRepository.save(newUser);

        return new GoogleLoginResponse(
                newUser.getUserId(),
                true);
    }

    private GoogleIdToken.Payload verifyToken(String idTokeString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new JacksonFactory())
                    .setAudience(Collections
                            .singletonList("450998180907-r7r2loc02s1gghfmf76tgtc0lnkdpcog.apps.googleusercontent.com"))
                    .build();
            GoogleIdToken idToken = verifier.verify(idTokeString);
            if (idToken == null) {
                throw new RuntimeException("Invalid token");
            }
            return idToken.getPayload();
        } catch (Exception e) {
            throw new RuntimeException("Token verification failed", e);
        }
    }

    public void updateUserName(UUID userId, String userName) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        user.setUserName(userName);
        userRepository.save(user);
    }
}