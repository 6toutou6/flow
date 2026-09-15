package com.company.flow.sys.flowdata.vo;

import lombok.Data;

/**
 * 分维度（个人 / 部门）数据展示的统计卡数据。
 * 同一份字段在两个维度下含义不同：
 * - 个人维度：任务/期次 = 我的；待办/已办 = 我作为处理人的节点数；人数 = 1
 * - 部门维度：任务/期次 = 本部门创建或参与的；待办/已办 = 部门成员的节点数；人数 = 部门成员数
 */
@Data
public class ScopedStatsVO {

    /** 任务数（个人=我的任务数；部门=本部门成员的任务数） */
    private Long totalTasks;

    /** 期次数（个人=我参与的期次数；部门=涉及本部门的期次数） */
    private Long totalPeriods;

    /** 进行中的期次数 */
    private Long runningPeriods;

    /** 已完成的期次数（期次下成员任务全部已结束） */
    private Long finishedPeriods;

    /** 待办数（处理人=本人/部门成员 且节点待处理） */
    private Long myTodoCount;

    /** 已办数（处理人=本人/部门成员 且节点已完成） */
    private Long doneCount;

    /** 人数（个人维度固定 1；部门维度=部门成员数） */
    private Long memberCount;

    /** 提交数（已提交的表单记录数） */
    private Long submitCount;
}
