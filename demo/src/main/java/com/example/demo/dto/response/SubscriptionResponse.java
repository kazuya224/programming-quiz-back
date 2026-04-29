package com.example.demo.dto.response;

import com.example.demo.entity.Subscription;

public record SubscriptionResponse(
        boolean isPremium,
        Subscription subscription) {
}