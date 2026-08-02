package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 任务流转进度项（el-steps 渲染）
 */
@Data
public class TaskProgressVO {
    private Long taskNodeId;
    private Long nodeId;
    private String nodeName;
    private Integer sortNum;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    private Long handlerUserId;
    /** 处理人姓名 */
    private String handlerName;
    /** 处理状态 0待处理 1已处理 */
    private Integer submitStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handleTime;
    private Long nextHandlerUserId;
    private String nextHandlerName;
    private Long formRecordId;
    /** 操作类型 0通过 1退回 */
    private Integer action;
    /** 退回原因（action=1 时回填） */
    private String rejectReason;
    /** 通过意见（action=0 时回填） */
    private String passComment;
    /** 该节点提交的表单数据（已处理时回填，用于点击查看历史） */
    private List<FormDataItemVO> formDataList;
}
