package com.company.flow.sys.flowdata.vo;

import lombok.Data;

/**
 * 数据后台统计卡数据（任务 → 期次 维度）
 */
@Data
public class DataStatsVO {
    /** 任务总数 */
    private Long totalTasks;
    /** 期次总数 */
    private Long totalPeriods;
    /** 进行中期次数（有进行中成员的期次） */
    private Long runningPeriods;
    /** 我的待办数 */
    private Long myTodoCount;
}
