package com.example.demo.controller;

import com.example.demo.dto.response.SubscriptionResponse;
import com.example.demo.entity.Subscription;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.SubscriptionService;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    // 状態取得
    @GetMapping("/me")
    public SubscriptionResponse get(@AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {

        User user = getUserFromJwt(jwt);

        boolean isPremium = subscriptionService.isPremium(user);
        Subscription sub = subscriptionService.getSubscription(user);
        System.out.println("currentPeriodEnd: " + sub.getCurrentPeriodEnd());
        System.out.println("now: " + LocalDateTime.now());

        return new SubscriptionResponse(isPremium, sub);
    }

    // 購入
    @PostMapping("/checkout")
    public Map<String, String> checkout(@AuthenticationPrincipal Jwt jwt) throws Exception {

        User user = getUserFromJwt(jwt);

        String url = subscriptionService.createCheckoutSession(user);

        return Map.of("url", url);
    }

    // 解約
    @PostMapping("/cancel")
    public void cancel(@AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {

        User user = getUserFromJwt(jwt);
        subscriptionService.cancel(user);
    }

    // 🔥 共通化（重要）
    private User getUserFromJwt(org.springframework.security.oauth2.jwt.Jwt jwt) {

        String userId = jwt.getSubject(); // ← 要確認

        return userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}