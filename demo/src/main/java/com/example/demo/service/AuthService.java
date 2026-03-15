package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.dto.SignupRequest; // 追加
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ログイン（既存）
    public Optional<User> login(String userName, String password) {
        return userRepository.findByUserName(userName)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }

    // 【新規】アカウント作成
    @Transactional
    public User signup(SignupRequest request) {
        // 1. 名前で検索し、リストとして取得する（1件以上あってもエラーにならないようにする）
        List<User> existingUsers = userRepository.findAllByUserName(request.getUserName());

        // 2. 1件でも存在すれば、登録を拒否する
        if (!existingUsers.isEmpty()) {
            throw new RuntimeException("このユーザー名は既に使用されています。別の名前を試してください。");
        }

        User newUser = new User();
        newUser.setUserName(request.getUserName());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(newUser);
    }
}