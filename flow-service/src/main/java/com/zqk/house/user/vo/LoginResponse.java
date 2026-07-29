package com.zqk.house.user.vo;

import com.zqk.house.user.entity.User;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private User userInfo;
    
    public LoginResponse(String token, User userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
} 