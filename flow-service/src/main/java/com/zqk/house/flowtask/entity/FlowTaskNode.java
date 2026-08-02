package com.zqk.house.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务流转节点记录（对应 flow.flow_task_node 表）
 * 任务实际流转记录，每个节点一条，记录处理人、处理状态、表单记录
 */
@Data
@TableName("flow_task_node")
public class FlowTaskNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long nodeId;
    private String nodeName;
    private Integer sortNum;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    /** 本节点处理人ID */
    private Long handlerUserId;
    /** 处理状态 0待处理 1已处理 */
    private Integer submitStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handleTime;
    /** 指定的下一节点处理人ID */
    private Long nextHandlerUserId;
    /** 本节点提交的表单记录ID flow_form_record.id */
    private Long formRecordId;
    /** 操作类型 0通过 1退回 */
    private Integer action;
    /** 退回原因（action=1 时填写） */
    private String rejectReason;
    /** 通过意见（action=0 时填写，非必填） */
    private String passComment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
