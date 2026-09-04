package com.company.flow.sys.flowtask.vo;

import lombok.Data;

/**
 * 任务处理页统计卡数据（当前登录用户）
 */
@Data
public class MyTodoStatsVO {
    /** 我的任务数（任务级分组数） */
    private Long taskCount;
    /** 待处理节点数 */
    private Long pendingNodeCount;
    /** 已完成节点数 */
    private Long doneNodeCount;
}
