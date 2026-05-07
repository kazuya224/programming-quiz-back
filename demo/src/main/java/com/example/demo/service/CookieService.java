package com.example.demo.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieService {

    @Value("${app.is-prod}")
    private boolean isProd;

    public void addTokenCookie(HttpServletResponse response, String token) {
        System.out.println("isProd=" + isProd);
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .secure(isProd)
                .sameSite(isProd ? "None" : "Lax")
                .build();

        // Set-Cookieヘッダーとして直接書き込む（SameSiteが確実に含まれる）
        response.addHeader("Set-Cookie", cookie.toString());
    }
}