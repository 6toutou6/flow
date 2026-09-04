package com.company.flow.sys.base.autuser.vo;

import lombok.Data;

/**
 * 登录响应：登录成功后返回用户基础信息（不含自增主键、不含密码）
 */
@Data
public class LoginResponse {
    /** 用户号 */
    private String yyytId;
    /** 中文姓名 */
    private String userName;
    private Long deptId;
    private String deptName;
    /** 是否超管（数据权限展示用） */
    private Boolean superAdmin;
}
