package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // パスワードハッシュ化の仕組みをBeanとして定義（どこからでも呼び出せるようにする）
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 本番環境を見据え、現時点では認証をバイパスしつつ構成を整える
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // APIとして使うため一旦無効化
                .cors(cors -> {
                })
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 開発中なので全許可。本番ではここを絞る
                );
        return http.build();
    }
}