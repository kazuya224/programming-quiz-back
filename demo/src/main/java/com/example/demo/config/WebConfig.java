// // src/main/java/com/example/demo/config/WebConfig.java
// package com.example.demo.config;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Configuration
// public class WebConfig implements WebMvcConfigurer {
// @Override
// public void addCorsMappings(CorsRegistry registry) {
// registry.addMapping("/**") // ←全部対象にする
// .allowedOrigins(
// "http://localhost:3000",
// "https://programing-quiz-zeta.vercel.app")
// .allowedMethods("*")
// .allowedHeaders("*")
// .allowCredentials(true);
// }
// }