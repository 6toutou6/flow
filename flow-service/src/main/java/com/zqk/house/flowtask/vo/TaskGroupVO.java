package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 任务组（一次下发 = 一个任务组）。任务级不含“当前处理人”，只有任务信息与聚合状态；
 * 下发给每个人的独立任务由 members 承载（各自有当前处理人/节点进度）。
 */
@Data
public class TaskGroupVO {

    /** 期次ID（flow_task_dispatch.id） */
    private Long dispatchId;
    /** 组内主任务ID（min-id 任务），用于新增人员等操作 */
    private Long primaryTaskId;
    /** 主任务自身状态（决定能否加人，非聚合状态） */
    private Integer primaryTaskStatus;
    /** 任务名称（期次的名称，从任务抄用） */
    private String taskName;
    private String taskDesc;
    private Long templateId;
    /** 所属任务ID（flow_dispatch.id） */
    private Long dispatchPlanId;
    /** 所属任务名称（flow_dispatch.task_name，列表展示用） */
    private String planName;
    /** 期次序号 */
    private Integer periodNo;
    /** 期次名称（如 2026-08） */
    private String periodName;
    /** 期次来源 0自动下发 1手动临时期次 */
    private Integer manualFlag;
    /** 下发时间（主任务创建时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date dispatchTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /** 聚合状态：1进行中 2已完成 3已作废（任一成员进行中→1；全作废→3；否则→2） */
    private Integer status;
    private Integer memberCount;
    private Integer runningCount;
    private Integer finishedCount;
    private Integer cancelledCount;

    /** 组内全部成员（仅 dispatch 详情接口填充） */
    private List<TaskMemberVO> members;
}
