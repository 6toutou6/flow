package com.company.flow.sys.base.deptAdmin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 部门管理员实体（对应 flow.dept_admin 表）
 * 用于管理各部门管理员权限：创建模板/任务等创建操作须为本部门管理员
 */
@Data
@TableName("dept_admin")
public class DeptAdmin {

    /** 管理员姓名 */
    private String adminName;

    /** 管理员用户号（yyyt_id） */
    private String adminYstId;

    /** 部门id */
    private String deptId;

    /** 部门名称 */
    private String deptName;

    /** 部门邮箱地址 */
    private String deptEmail;

    /** 部门邮箱密码 */
    private String deptEmailPwd;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date modifiedTime;
}
