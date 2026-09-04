package com.company.flow.sys.flowdata.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 按人员展示：某人参与的任务链上「全部节点」行（不再只是该人员本人提交的节点）。
 * 前端按 taskId 分组渲染为一条条任务链面板，链内节点按 sortNum 流程顺序展示。
 */
@Data
public class PersonNodeVO {
    // ===== 任务链分组信息（flow_task + flow_task_dispatch） =====
    /** 成员任务ID（flow_task.id，前端以此分组为一条链） */
    private String taskId;
    /** 任务名称 */
    private String taskName;
    /** 任务状态（进行中/已结束/已作废） */
    private String taskStatus;
    /** 期次ID（flow_task_dispatch.id，未关联期次的旧数据为空） */
    private String dispatchId;
    /** 期次名称 */
    private String periodName;
    private Integer periodNo;

    // ===== 节点实例信息（flow_task_node） =====
    private String taskNodeId;
    /** 节点名称（任务流转节点快照名，模板重建后仍可显示） */
    private String nodeName;
    /** 节点顺序（链内流程顺序） */
    private Integer sortNum;
    /** 本节点处理人ID */
    private String handlerUserId;
    /** 本节点处理人姓名 */
    private String handlerName;
    /** 处理状态 0待处理 1已处理 */
    private Integer submitStatus;
    /** 操作类型 0通过 1退回 */
    private Integer action;
    /** 退回原因（action=1 时） */
    private String rejectReason;
    /** 通过意见（action=0 时） */
    private String passComment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handleTime;

    /** 节点提交的表单记录ID（已处理节点可据此拉取表单详情；待处理为空） */
    private String recordId;
}
