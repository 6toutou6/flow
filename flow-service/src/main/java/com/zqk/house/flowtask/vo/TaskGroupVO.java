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

    /** 下发批次ID（= 组内首条任务ID） */
    private Long dispatchId;
    /** 组内主任务ID（min-id 任务），用于新增人员等操作 */
    private Long primaryTaskId;
    /** 主任务自身状态（决定能否加人，非聚合状态） */
    private Integer primaryTaskStatus;
    /** 任务名称（取主任务） */
    private String taskName;
    private String taskDesc;
    private Long templateId;
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
