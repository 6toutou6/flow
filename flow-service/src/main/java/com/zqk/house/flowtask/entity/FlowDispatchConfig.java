package com.zqk.house.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务下发配置（flow.flow_dispatch_config）：每任务一份，独立于任务表存储。
 * 周期类型/触发日/截止天数/催办天数单独建表，避免用户反复配置导致错误。
 * 生成期次时抄用此配置计算期次与截止日期。
 */
@Data
@TableName("flow_dispatch_config")
public class FlowDispatchConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属任务ID（flow_dispatch.id，每任务一份） */
    private Long taskId;

    /** 周期类型 1每周 2每月 3每季度 4单次下发 */
    private Integer cycleType;

    /** 触发日：周(1-7周一=1)/月(1-31)/季(1-31) */
    private Integer cycleDay;

    /** 每期次截止：触发日后N天截止（单次下发按下发日起算） */
    private Integer deadlineDays;

    /** 截止前N天自动催办（仅写日志，不真实通知） */
    private Integer urgeDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
