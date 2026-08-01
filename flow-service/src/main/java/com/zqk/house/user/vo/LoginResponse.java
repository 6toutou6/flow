package com.zqk.house.user.vo;

import com.zqk.house.sysuser.entity.SysUser;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private SysUser userInfo;

    public LoginResponse(String token, SysUser userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}
