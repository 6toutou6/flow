package com.company.flow.sys.base.autuser.vo;

import lombok.Data;

/**
 * 当前登录用户信息（登录成功后由 AuthInterceptor 从会话写入 ThreadLocal）
 * 展示格式统一为「姓名（用户号）」
 */
@Data
public class LoginUser {
    /** 用户号 yyyt_id（一切匹配/关联/通知用） */
    private String yyytId;
    /** 中文姓名 */
    private String userName;
    private Long deptId;
    private String deptName;
    /** 是否超管（由后端配置文件 system.admin.userIds 判定） */
    private Boolean superAdmin;
}
