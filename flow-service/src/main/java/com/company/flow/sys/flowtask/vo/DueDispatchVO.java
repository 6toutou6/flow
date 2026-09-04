package com.company.flow.sys.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 到期待下发的期次信息（周期任务下一期次窗口开始时间已到且尚未生成）
 */
@Data
public class DueDispatchVO {
    private String taskId;
    private String taskName;
    /** 期次序号 */
    private Integer periodNo;
    /** 期间标识（周/月/季） */
    private String periodKey;
    /** 期次名称 */
    private String periodName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
}
