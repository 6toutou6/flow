package com.company.flow.sys.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 超管配置：从 application.yml 的 system.admin.userIds 读取（用户号 yyyt_id 列表，逗号分隔）。
 * 命中即超管，超管可查看/操作全部数据。
 */
@Data
@Component
@ConfigurationProperties(prefix = "system.admin")
public class AdminProperties {

    /** 超管用户号列表 */
    private String userIds;

    public boolean isSuperAdmin(String yyytId) {
        if (!StringUtils.hasText(userIds) || !StringUtils.hasText(yyytId)) {
            return false;
        }
        for (String id : userIds.split(",")) {
            if (yyytId.trim().equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 超管用户号集合（供批量判断） */
    public List<String> getUserIdList() {
        List<String> list = new ArrayList<>();
        if (StringUtils.hasText(userIds)) {
            for (String id : userIds.split(",")) {
                if (StringUtils.hasText(id)) {
                    list.add(id.trim());
                }
            }
        }
        return list;
    }
}
