package com.zqk.house.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 主任务（对应 flow.flow_task_dispatch 表）：一次下发 = 一条主任务。
 * 成员（flow_task）删除后主任务仍保留；无成员时可在数据后台单独删除。
 */
@Data
@TableName("flow_task_dispatch")
public class FlowTaskDispatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    /** 锁定模板版本 */
    private Integer templateVersion;
    private String taskName;
    private String taskDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
