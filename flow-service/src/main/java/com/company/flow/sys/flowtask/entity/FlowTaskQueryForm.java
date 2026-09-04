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
}
