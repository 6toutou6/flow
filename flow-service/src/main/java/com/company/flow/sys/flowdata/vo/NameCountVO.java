package com.company.flow.sys.flowdata.vo;

import lombok.Data;

/**
 * 图表展示通用项（名称 + 数值）
 */
@Data
public class NameCountVO {
    private String name;
    private Long value;
}
