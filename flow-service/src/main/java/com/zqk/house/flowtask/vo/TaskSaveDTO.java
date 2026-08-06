package com.zqk.house.flowtask.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 任务创建/更新入参：任务基本信息 + 下发周期配置 + 模板配置信息 + 人员
 * 任务配置好后，后续生成期次抄用 templateData 与 memberIds
 */
@Data
public class TaskSaveDTO {
    private Long id;
    private Long templateId;
    private String taskName;
    private String taskDesc;
    /** 模板级字段值（fieldId → value，任务级配置，期次生成时抄用） */
    private Map<Long, String> templateData;
    /** 周期类型 1每周 2每月 3每季度 4单次下发 */
    private Integer cycleType;
    /** 触发日：周(1-7周一=1)/月(1-31)/季(1-31) */
    private Integer cycleDay;
    /** 每期次截止：触发日后N天截止 */
    private Integer deadlineDays;
    /** 截止前N天自动催办（仅日志） */
    private Integer urgeDays;
    /** 0停用 1启用 */
    private Integer status;
    /** 任务人员（用户ID列表） */
    private List<Long> memberIds;
}
