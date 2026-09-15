package com.company.flow.sys.flowtask.vo;

import lombok.Data;

/**
 * 批量导入人员：Excel 中一行的解析结果（校通过后返回给前端，由前端并入人员配置列表）。
 */
@Data
public class ImportMemberVO {

    /** 用户号（aut_user.yyyt_id） */
    private String yyytId;

    /** 中文姓名（取自 aut_user，以库中为准） */
    private String userName;

    /** 所属部门（取自 aut_user，实时带出） */
    private String deptName;

    /** 任务名（Excel 留空时按「下发给{姓名}的任务」生成） */
    private String taskName;
}
