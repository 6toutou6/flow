package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务人员信息（flow_task_member JOIN sys_user）
 */
@Data
public class TaskMemberInfoVO {

    private Long userId;
    private String realName;
    private String empNo;
    private String deptName;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
