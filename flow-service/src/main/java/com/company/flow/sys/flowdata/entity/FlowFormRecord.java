package com.company.flow.sys.flowdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 表单填报主记录（对应 flow.flow_form_record 表）
 */
@Data
@TableName("flow_form_record")
public class FlowFormRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String taskId;
    private String templateId;
    /** 节点ID flow_template_node.id */
    private String nodeId;
    /** 任务流转节点ID flow_task_node.id */
    private String taskNodeId;
    private String userId;
    /** 填报人姓名（冗余） */
    private String userName;
    /** 1正常 2作废 */
    private Integer recordStatus;
    /** 0正式提交 1草稿 */
    private Integer isDraft;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
