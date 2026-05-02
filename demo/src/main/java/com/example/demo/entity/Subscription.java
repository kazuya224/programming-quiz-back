package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 nullable = true（DDL通り）
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "stripe_subscription_id", nullable = false, unique = true)
    private String stripeSubscriptionId;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_period_end", nullable = true)
    private LocalDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}