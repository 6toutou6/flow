package com.company.flow.sys.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务（对应 flow.flow_dispatch 表）
 * 任务 → 期次 → 人员：一个任务配置好模板、模板配置信息（template_data）、下发周期与人员，
 * 后续按期次（flow_task_dispatch）自动/手动下发，每次生成期次都抄用任务的配置信息与人员。
 */
@Data
@TableName("flow_dispatch")
public class FlowDispatch {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String templateId;
    /** 模板级字段值 JSON（{fieldId: value}，任务级配置，生成期次时抄用） */
    private String templateData;
    /** 任务名称（如：巡查问题整改） */
    private String taskName;
    private String taskDesc;
    /** 周期类型 1每周 2每月 3每季度 4单次下发（下发配置已独立成表 flow_dispatch_config，此字段由查询/回填填充） */
    @TableField(exist = false)
    private Integer cycleType;
    /** 触发日：周(1-7周一=1)/月(1-31)/季(1-31)（来自 flow_dispatch_config） */
    @TableField(exist = false)
    private Integer cycleDay;
    /** 每期次截止：触发日后N天截止（单次下发按下发日起算）（来自 flow_dispatch_config） */
    @TableField(exist = false)
    private Integer deadlineDays;
    /** 截止前N天自动催办（仅写日志，不真实通知）（来自 flow_dispatch_config） */
    @TableField(exist = false)
    private Integer urgeDays;
    /** 0停用 1启用 */
    private String status;
    private String creatorId;
    /** 创建人部门ID（部门内可见；样例公共可见） */
    private Long deptId;
    /** 1=样例(公共可见不可改) 0=普通 */
    private Integer isSample;

    /** 创建人姓名（冗余，随 creatorId 一并入库） */
    private String creatorName;
    /** 创建人部门名称（列表展示，非数据库字段） */
    @TableField(exist = false)
    private String deptName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /** 任务下已生成的期次数（列表查询填充） */
    @TableField(exist = false)
    private Integer periodCount;
    /** 任务下配置的人员数（列表查询填充） */
    @TableField(exist = false)
    private Integer memberCount;
}
