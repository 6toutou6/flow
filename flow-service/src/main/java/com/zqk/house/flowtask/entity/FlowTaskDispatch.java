package com.zqk.house.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 期次（对应 flow.flow_task_dispatch 表）：任务按周期自动或手动生成的每一期。
 * 生成期次时抄用任务的模板配置信息（template_data）与人员，为每位人员创建独立提交任务（flow_task）；
 * 成员（flow_task）删除后期次仍保留；无成员时可在数据后台单独删除。
 */
@Data
@TableName("flow_task_dispatch")
public class FlowTaskDispatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属任务ID（flow_dispatch.id） */
    private Long taskId;
    private Long templateId;
    /** 锁定模板版本 */
    private Integer templateVersion;
    /** 旧下发计划ID（flow_dispatch.id，已废弃，改为 task_id 关联） */
    private Long dispatchPlanId;
    /** 期次序号（同一任务下递增） */
    private Integer periodNo;
    /** 期次名称（如 2026-08） */
    private String periodName;
    /** 期间标识（自动下发防重复检测，如 2026-Q3 / 2026-08 / 2026-W31；手动临时期次为空） */
    private String periodKey;
    /** 期次来源 0自动下发 1手动临时期次 */
    private Integer manualFlag;
    private String taskName;
    private String taskDesc;
    /** 模板级字段值 JSON（{fieldId: value}，下发时创建人赋值，新增成员任务时复制） */
    private String templateData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
