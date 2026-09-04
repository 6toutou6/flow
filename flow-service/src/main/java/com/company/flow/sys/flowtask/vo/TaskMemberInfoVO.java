package com.company.flow.sys.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务人员信息（flow_task_member JOIN sys_user）
 */
@Data
public class TaskMemberInfoVO {

    private String yyytId;
    private String userName;
    /** 该成员每期生成的任务名称（名单配置，默认「下发给{姓名}的任务」） */
    private String taskName;
    private String empNo;
    private String deptName;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
