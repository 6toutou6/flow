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
    /** 下一步处理人提示（提交节点选择下一处理人时展示） */
    private String nextHandlerTip;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;

    /** 待办状态 0待处理 1已处理（仅查看） */
    private Integer todoStatus;

    /** 所属期次ID flow_task_dispatch.id（存量无期次任务为 null） */
    private Long dispatchId;
    /** 计划任务ID flow_dispatch.id（所属任务，任务处理按任务→期次展示的分组依据） */
    private Long planId;
    /** 期次名称 */
    private String periodName;
    /** 期次编号 */
    private Integer periodNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date periodStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date periodEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date taskCreateTime;
}
