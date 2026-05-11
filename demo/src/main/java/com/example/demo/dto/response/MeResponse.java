package com.example.demo.dto.response;

import java.util.UUID;

public class MeResponse {
    private String userName;
    private boolean hasAnswered;

    public MeResponse(String userName, boolean hasAnswered) {
        this.userName = userName;
        this.hasAnswered = hasAnswered;
    }

    public String getUserName() {
        return userName;
    }

    public boolean getHasAnswered() {
        return hasAnswered;
    }
}
