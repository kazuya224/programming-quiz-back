package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserName(String userName);

    List<User> findAllByUserName(String userName);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);
}
