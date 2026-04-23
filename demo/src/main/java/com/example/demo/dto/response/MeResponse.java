package com.example.demo.dto.response;

import java.util.UUID;

public class MeResponse {
    private String userName;

    public MeResponse(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
