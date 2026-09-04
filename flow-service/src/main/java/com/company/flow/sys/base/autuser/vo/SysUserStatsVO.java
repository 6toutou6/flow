package com.company.flow.sys.base.autuser.vo;

import lombok.Data;

/**
 * 用户管理页统计卡数据
 */
@Data
public class SysUserStatsVO {
    /** 用户总数 */
    private Long total;
    /** 正常用户数 */
    private Long normalCount;
    /** 禁用用户数 */
    private Long disabledCount;
    /** 部门数（有用户的部门） */
    private Long deptCount;
}
