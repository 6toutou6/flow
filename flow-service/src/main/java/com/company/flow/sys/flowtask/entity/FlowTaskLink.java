package com.company.flow.sys.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务关联（汇总 ← 收集），对应 flow.flow_task_link 表。
 * 一个汇总任务（成员任务或任务配置）可关联多个收集任务（任务配置/期次），双向可跳转查看。
 */
@Data
@TableName("flow_task_link")
public class FlowTaskLink {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 来源任务配置ID（flow_dispatch.id；配置级关联：管理员在任务管理侧建立，跨期次可见） */
    private String sourceDispatchId;
    /** 来源成员任务ID（flow_task.id；成员级关联：处理人从自己收到的成员任务发起） */
    private String sourceTaskId;
    /** 来源期次ID（flow_task_dispatch.id，冗余，便于反向聚合） */
    private String sourcePeriodId;
    /** 目标收集任务配置ID（flow_dispatch.id） */
    private String targetDispatchId;
    /** 目标收集期次ID（flow_task_dispatch.id；可空：配置级关联未落到具体期次） */
    private String targetPeriodId;
    /** 目标成员任务ID（flow_task.id；可空：直接关联到我收到的某条任务时填写，冗余所属配置/期次） */
    private String targetTaskId;
    /** 关联类型 collect=汇总收集（预留一般关联扩展） */
    private String linkType;
    /** 备注 */
    private String remark;
    /** 创建人用户号 */
    private String creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
