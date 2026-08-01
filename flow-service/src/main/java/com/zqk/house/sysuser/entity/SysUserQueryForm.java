package com.zqk.house.sysuser.entity;

import lombok.Data;

/**
 * 系统用户分页查询参数
 */
@Data
public class SysUserQueryForm {
    private Integer page;
    private Integer limit;
    private String username;
    private String empNo;
    private String realName;
    private String deptName;
    private Integer status;
}
