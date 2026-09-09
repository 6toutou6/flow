package com.company.flow.sys.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务关联展示对象（汇总 ← 收集），含收集任务目标信息与收集进度聚合
 */
@Data
public class TaskLinkVO {

    private String id;
    /** 关联类型 collect=汇总收集 */
    private String linkType;
    private String remark;

    /** 来源任务配置ID（配置级） */
    private String sourceDispatchId;
    /** 来源任务配置名称（配置级关联展示） */
    private String sourceDispatchName;
    /** 来源成员任务ID（成员级） */
    private String sourceTaskId;
    /** 来源成员任务名称（成员级关联展示） */
    private String sourceTaskName;
    /** 来源期次ID（冗余） */
    private String sourcePeriodId;
    /** 来源期次名称 */
    private String sourcePeriodName;

    /** 目标收集任务配置ID */
    private String targetDispatchId;
    /** 目标收集任务配置名称 */
    private String targetDispatchName;
    /** 目标成员任务ID（直接关联到我收到的某条任务时） */
    private String targetTaskId;
    /** 目标成员任务名称 */
    private String targetTaskName;
    /** 目标收集期次ID（配置级未落到期次时为空） */
    private String targetPeriodId;
    /** 目标收集期次名称 */
    private String targetPeriodName;
    /** 目标期次截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date targetEndTime;
    /** 目标任务开始时间（成员任务自身起止） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date targetStartTime;
    /** 目标任务流程节点链（status：0未开始 1已完成 2进行中），供表格流程进度列展示 */
    private java.util.List<TodoNodeVO> targetChain;
    /** 收集人数（目标期次下成员任务数） */
    private Integer memberCount;
    /** 已收集份数（目标期次下状态=已结束的成员任务数） */
    private Integer doneCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
