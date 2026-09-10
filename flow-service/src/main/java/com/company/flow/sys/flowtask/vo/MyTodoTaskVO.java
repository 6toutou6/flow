package com.company.flow.sys.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 我的任务（任务级分组）：任务 → 期次 → 待办节点 两级展示
 */
@Data
public class MyTodoTaskVO {
    private String taskId;
    private String taskName;
    private String templateId;
    private String templateName;

    /** 涉及我的期次数 */
    private Integer periodCount;
    /** 其中归属人（owner_id）是我的期次数——决定能否发起任务交接 */
    private Integer myPeriodCount;
    /** 我在该任务下的节点席位数（含已提交的历史节点）——交接范围里的「参与记录」条数 */
    private Integer myNodeCount;
    /** 我的待处理节点总数 */
    private Integer pendingCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date taskCreateTime;

    /** 该任务下我的期次分组（未关联期次的存量节点归入「无期次」组） */
    private List<MyTodoPeriodVO> periods = new ArrayList<>();
}
