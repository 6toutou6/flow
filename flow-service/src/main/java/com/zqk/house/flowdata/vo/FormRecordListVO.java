package com.zqk.house.flowdata.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 填报记录列表项（联表 sys_user + flow_template + flow_template_node）
 */
@Data
public class FormRecordListVO {
    private Long id;
    private Long taskId;
    private Long templateId;
    private Long nodeId;
    private Long taskNodeId;
    private Long userId;
    private Integer recordStatus;
    private Integer isDraft;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 联表 sys_user */
    private String userName;
    private String userEmpNo;
    /** 联表 flow_template */
    private String templateName;
    private String category;
    /** 联表 flow_template_node */
    private String nodeName;
}
