package com.company.flow.sys.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务人员配置（对应 flow.flow_task_member 表）
 * 任务创建时配置人员，生成期次时为每位人员创建独立提交任务；人员可在任务中变更
 */
@Data
@TableName("flow_task_member")
public class FlowTaskMember {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 任务ID（flow_dispatch.id） */
    private String taskId;
    /** 人员用户ID（sys_user.id） */
    private String userId;
    /** 人员姓名（冗余，随 userId 一并入库） */
    private String userName;
    /** 该成员每期生成的任务名称（冗余配置；默认「下发给{姓名}的任务」，生成期次时逐人沿用，可人工调整） */
    private String taskName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
