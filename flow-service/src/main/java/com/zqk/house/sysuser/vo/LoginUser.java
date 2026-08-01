package com.zqk.house.sysuser.vo;

import lombok.Data;

/**
 * 当前登录用户信息（存入 ThreadLocal，供后端任意方法获取）
 */
@Data
public class LoginUser {
    private Long id;
    private String username;
    private String empNo;
    private String realName;
    private Long deptId;
    private String deptName;
}
