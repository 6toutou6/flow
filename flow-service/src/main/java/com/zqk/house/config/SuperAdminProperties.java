package com.zqk.house.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 超管配置：从 application.yml 的 system.super-admin 读取。
 * 登录用户的「员工号 + 姓名」同时匹配才判定为超管；超管可查看全部数据、设置样例。
 */
@Data
@Component
@ConfigurationProperties(prefix = "system.super-admin")
public class SuperAdminProperties {

    /** 超管员工号 */
    private String empNo;

    /** 超管姓名 */
    private String realName;

    public boolean isSuperAdmin(String empNo, String realName) {
        return StringUtils.hasText(this.empNo) && this.empNo.equals(empNo)
                && StringUtils.hasText(this.realName) && this.realName.equals(realName);
    }
}
