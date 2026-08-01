package com.zqk.house.sysuser.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 系统用户实体（对应 flow.sys_user 表）
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号 */
    private String username;

    /** 员工号 */
    private String empNo;

    /** 员工姓名 */
    private String realName;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 密码（默认查询不返回，新增/更新时才写入） */
    @TableField(select = false)
    private String password;

    /** 手机号 */
    private String phone;

    /** 状态 0禁用 1正常 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
