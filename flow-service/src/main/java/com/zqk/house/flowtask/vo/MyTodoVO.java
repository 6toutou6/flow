package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 我的待办列表项
 */
@Data
public class MyTodoVO {
    /** 任务流转节点ID flow_task_node.id */
    private Long taskNodeId;
    private Long taskId;
    private String taskName;
    private Long templateId;
    private String templateName;
    private Long currentNodeId;
    private String nodeName;
    private String nodeTips;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date taskCreateTime;
}
