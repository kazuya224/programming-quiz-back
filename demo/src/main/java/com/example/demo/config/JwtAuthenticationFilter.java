// package com.example.demo.config;

// import java.io.IOException;
// import java.util.Collections;
// import java.util.UUID;

// import org.springframework.beans.factory.annotation.Autowired;
// import
// org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;

// import com.example.demo.service.JwtService;

// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;

// @Component
// public class JwtAuthenticationFilter extends OncePerRequestFilter {

// @Autowired
// private JwtService jwtService;

// @Override
// protected void doFilterInternal(HttpServletRequest request,
// HttpServletResponse response,
// FilterChain filterChain)
// throws ServletException, IOException {

// String header = request.getHeader("Authorization");

// if (header != null && header.startsWith("Bearer ")) {
// String token = header.substring(7);

// try {
// UUID userId = jwtService.extractUserId(token);

// UsernamePasswordAuthenticationToken auth = new
// UsernamePasswordAuthenticationToken(
// userId,
// null,
// Collections.emptyList());

// SecurityContextHolder.getContext().setAuthentication(auth);

// } catch (Exception e) {
// // トークン不正 → 無視 or 401にする
// }
// }

// filterChain.doFilter(request, response);
// }
// }
