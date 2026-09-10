package com.company.flow.sys.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 填报任务（对应 flow.flow_task 表）
 */
@Data
@TableName("flow_task")
public class FlowTask {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String templateId;
    /** 下发批次ID（同一次下发/跟进共享，= 组内首条任务ID，用于「任务→人员」分组） */
    private String dispatchId;
    /** 锁定模板版本，防止模板修改影响已有任务 */
    private Integer templateVersion;
    private String taskName;
    private String taskDesc;
    /** 模板级字段值 JSON（{fieldId: value}，下发时创建人赋值，处理人与后台可见） */
    private String templateData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    /** 1进行中 2已结束 3作废 */
    private String status;
    /** 当前节点ID（流转指针） */
    private String currentNodeId;
    /** 当前处理人ID（流转指针） */
    private String currentHandlerId;
    /** 当前归属人（人员交接后随人变更；「我的任务」可见性 = owner 或 节点处理人） */
    private String ownerId;
    /** 已完成节点数 */
    private Integer finishedNodeCount;
    /** 总节点数 */
    private Integer totalNodeCount;
    private String creatorId;
    /** 创建人姓名（冗余） */
    private String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
