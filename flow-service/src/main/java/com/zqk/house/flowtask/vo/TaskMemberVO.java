package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务组成员 = 一条独立的人员任务（flow_task）。
 * 成员身份（owner）= 该任务起始节点处理人（被下发的那个人的独立任务）；
 * 当前处理人/当前节点/进度为该任务自身的流转指针。
 */
@Data
public class TaskMemberVO {

    private Long taskId;
    private String taskName;
    private Long templateId;
    /** 该成员任务状态：1进行中 2已完成 3已作废 */
    private Integer status;

    /** 被下发人（起始节点处理人） */
    private Long ownerUserId;
    private String ownerName;
    private String ownerEmpNo;
    private String ownerDept;

    /** 当前处理人（进行中时有值；已完成可为空） */
    private Long currentHandlerId;
    private String currentHandlerName;
    /** 当前节点（悬空引用时为空） */
    private Long currentNodeId;
    private String currentNodeName;

    private Integer finishedNodeCount;
    private Integer totalNodeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
