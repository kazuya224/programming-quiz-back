package com.example.demo.dto.response;

import java.util.UUID;

public class GoogleLoginResponse {

    private UUID userId;
    private boolean isNewUser;

    public GoogleLoginResponse(UUID userId, boolean isNewUser) {
        this.userId = userId;
        this.isNewUser = isNewUser;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isNewUser() {
        return isNewUser;
    }
}