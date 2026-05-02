package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "password")
    private String password;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "provider_id", unique = true)
    private String providerId;

    @Column(name = "provider")
    private String provider;

    // 🔥 Stripe連携で最重要
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "trial_used", nullable = false, columnDefinition = "boolean default false")
    private boolean trialUsed = false;

    // 🔥 timestamptz対応
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}