package com.company.flow.sys.flowtask.entity;

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

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String taskId;
    private String nodeId;
    /** 期次节点快照ID flow_task_dispatch_node.id（无则回退模板解析） */
    private String dispatchNodeId;
    private String nodeName;
    private Integer sortNum;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    /** 本节点处理人ID */
    private String handlerUserId;
    /** 本节点处理人姓名（冗余） */
    private String handlerUserName;
    /** 处理状态 0待处理 1已处理 */
    private Integer submitStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handleTime;
    /** 指定的下一节点处理人ID */
    private String nextHandlerUserId;
    /** 指定的下一节点处理人姓名（冗余） */
    private String nextHandlerUserName;
    /** 本次流转所选下一节点处理人全集（JSON [{id,name}]，多选保留，退回重做后回填用） */
    private String nextHandlerIds;
    /** 本节点提交的表单记录ID flow_form_record.id */
    private String formRecordId;
    /** 处理人填写的任务基础字段值（JSON fieldId→value，模板级字段 fieldRole=2） */
    private String baseData;
    /** 操作类型 0通过 1退回 */
    private Integer action;
    /** 退回原因（action=1 时填写） */
    private String rejectReason;
    /** 通过意见（action=0 时填写，非必填） */
    private String passComment;
    /** 转办原处理人用户号（被转办时记录） */
    private String transferFromUserId;
    /** 转办原处理人姓名 */
    private String transferFromUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
