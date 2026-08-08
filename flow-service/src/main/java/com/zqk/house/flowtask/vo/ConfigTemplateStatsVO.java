package com.zqk.house.flowtask.vo;

import lombok.Data;

/**
 * 下发配置模板页统计卡数据（可见性过滤后）
 */
@Data
public class ConfigTemplateStatsVO {
    /** 配置模板总数（可见范围） */
    private Long total;
    /** 样例数（样例公共可见） */
    private Long sampleCount;
    /** 我创建的模板数 */
    private Long mineCount;
}
