package com.company.flow.sys.base.autuser.entity;

import lombok.Data;

/**
 * 系统用户分页查询参数（管理页）
 */
@Data
public class UserQueryForm {
    private Integer page;
    private Integer limit;
    private String yyytId;
    private String userName;
    private String deptName;
    private Integer status;
}
