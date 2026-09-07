package com.company.flow.sys.flowtask.entity;

import lombok.Data;

@Data
public class FlowTaskQueryForm {
    private Integer page;
    private Integer limit;
    /** 任务ID过滤（数据后台第二层：某任务下的期次列表） */
    private String taskId;
    private String taskName;
    private String status;
    /** 任务类型：1 仅样例 / 0 仅普通 / null 全部 */
    private Integer taskType;
    /** 模板名称（模糊） */
    private String templateName;
    /** 创建时间范围（yyyy-MM-dd） */
    private String createStart;
    private String createEnd;
}
