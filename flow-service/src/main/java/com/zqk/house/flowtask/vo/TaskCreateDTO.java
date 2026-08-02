package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务下发入参：模板ID + 任务信息 + 一个或多个首节点处理人
 * 顺序流转模型：指定首节点（开始节点）的处理人，每个处理人会创建一个独立的任务实例，各自按流程流转
 */
@Data
public class TaskCreateDTO {
    private Long templateId;
    private String taskName;
    private String taskDesc;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    /** 首个（开始）节点处理人ID列表（支持批量下发，每人一个独立任务） */
    private List<Long> firstHandlerIds;
    /** 模板级字段值（fieldId → value，创建人下发时赋值，如规章制度/采购说明） */
    private Map<Long, String> templateData;
}
