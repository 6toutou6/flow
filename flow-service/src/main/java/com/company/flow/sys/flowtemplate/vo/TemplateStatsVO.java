package com.company.flow.sys.flowtemplate.vo;

import lombok.Data;

/**
 * 模板管理页统计卡数据
 */
@Data
public class TemplateStatsVO {
    private Long total;
    private Long active;
    /** 本月模板更新率 % */
    private Double monthlyUpdateRate;
}
