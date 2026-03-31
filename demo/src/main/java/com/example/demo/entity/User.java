package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "user_name", length = 64)
    private String userName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 【仕組み化】ユーザーに紐づく学習履歴を一括取得できるようにする（双方向リレーション）
    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL)
    private List<UserProgress> progressLogs;
}