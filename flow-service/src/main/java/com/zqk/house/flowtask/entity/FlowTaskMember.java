package com.zqk.house.flowtask.entity;

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

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID（flow_dispatch.id） */
    private Long taskId;
    /** 人员用户ID（sys_user.id） */
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
