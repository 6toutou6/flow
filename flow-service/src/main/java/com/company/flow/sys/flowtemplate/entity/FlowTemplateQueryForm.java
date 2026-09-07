package com.company.flow.sys.flowtemplate.entity;

import lombok.Data;

@Data
public class FlowTemplateQueryForm {
    private Integer page;
    private Integer limit;
    private String templateName;
    private String category;
    private String status;
    /** 样例筛选：1 仅样例 / 0 仅普通 / null 全部 */
    private Integer isSample;
    /** 更新时间范围（yyyy-MM-dd） */
    private String updateStart;
    private String updateEnd;
}
