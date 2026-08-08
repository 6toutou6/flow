package com.zqk.house.flowdata.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 按人员展示：某人单条历史提交记录（联表 flow_task + flow_task_dispatch + flow_task_node）
 */
@Data
public class PersonRecordVO {
    private Long id;
    private Long taskId;
    /** 任务名称 */
    private String taskName;
    /** 期次ID（flow_task_dispatch.id） */
    private Long dispatchId;
    /** 期次名称 */
    private String periodName;
    private Integer periodNo;
    /** 节点名称（取任务流转节点快照名，模板重建后仍可显示） */
    private String nodeName;
    private Integer recordStatus;
    private Integer isDraft;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;
}
