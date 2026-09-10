package com.company.flow.sys.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 任务流转进度项（el-steps 渲染）
 */
@Data
public class TaskProgressVO {
    private String taskNodeId;
    private String nodeId;
    private String nodeName;
    private Integer sortNum;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    private String handlerUserId;
    /** 处理人姓名 */
    private String handlerName;
    /** 交接留痕：本节点原处理人（发生任务交接时回填，界面展示为「接手人（原处理人 移交）」） */
    private String transferFromUserId;
    private String transferFromUserName;
    /** 处理状态 0待处理 1已处理 */
    private Integer submitStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handleTime;
    private String nextHandlerUserId;
    private String nextHandlerName;
    /** 本次流转所选下一节点处理人全集（JSON [{id,name}]，多选保留，退回重做后回填用） */
    private String nextHandlerIds;
    private String formRecordId;
    /** 操作类型 0通过 1退回 */
    private Integer action;
    /** 退回原因（action=1 时回填） */
    private String rejectReason;
    /** 通过意见（action=0 时回填） */
    private String passComment;
    /** 该节点提交的表单数据（已处理时回填，用于点击查看历史） */
    private List<FormDataItemVO> formDataList;
    /** 该节点处理人填写的任务基础字段（fieldRole=2，已处理时回填） */
    private List<FormDataItemVO> baseDataList;
}
