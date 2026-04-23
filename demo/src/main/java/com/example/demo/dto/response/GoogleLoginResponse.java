package com.example.demo.dto.response;

import java.util.UUID;

public class GoogleLoginResponse {

    private String token;
    private boolean isNewUser;
    private String userName;

    public GoogleLoginResponse(String token, boolean isNewUser, String userName) {
        this.token = token;
        this.isNewUser = isNewUser;
        this.userName = userName;
    }

    public String getToken() {
        return token;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public String getUserName() {
        return userName;
    }
}