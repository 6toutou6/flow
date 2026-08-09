package com.zqk.house.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务流转通知/催办日志（对应 flow.flow_task_log 表）
 * 节点流转时记录一条通知日志；管理员催办时记录一条催办日志
 */
@Data
@TableName("flow_task_log")
public class FlowTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成员任务ID */
    private Long taskId;
    /** 下发批次ID */
    private Long dispatchId;
    /** 任务流转节点ID */
    private Long taskNodeId;
    /** 模板节点ID */
    private Long nodeId;
    /** 日志类型 1流转通知 2催办 3待办提醒 */
    private Integer logType;
    /** 日志内容 */
    private String content;
    /** 目标处理人ID */
    private Long handlerUserId;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
