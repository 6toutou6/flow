package com.company.flow.sys.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 下发配置模板（flow.flow_dispatch_config_template）：一套可长期复用的下发配置。
 * 新建任务时从中选择一套拉取填充到任务自己的 flow_dispatch_config，避免反复配置导致错误。
 */
@Data
@TableName("flow_dispatch_config_template")
public class FlowDispatchConfigTemplate {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 配置名称（用户可辨识，如「季度整改」） */
    private String configName;

    /** 周期类型 1每周 2每月 3每季度 4单次下发 */
    private Integer cycleType;

    /** 触发日：周(1-7周一=1)/月(1-31) */
    private Integer cycleDay;

    /** 每期次截止：触发日后N天截止（单次下发按下发日起算） */
    private Integer deadlineDays;

    /** 截止前N天自动催办（仅写日志，不真实通知） */
    private Integer urgeDays;

    /** 备注 */
    private String remark;

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
}
