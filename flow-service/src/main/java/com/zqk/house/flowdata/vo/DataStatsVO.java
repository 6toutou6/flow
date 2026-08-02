package com.zqk.house.flowdata.vo;

import lombok.Data;

/**
 * 数据后台统计卡数据（任务维度）
 */
@Data
public class DataStatsVO {
    /** 总任务数 */
    private Long totalTasks;
    /** 进行中任务数 */
    private Long runningTasks;
    /** 已完成任务数 */
    private Long finishedTasks;
    /** 我的待办数 */
    private Long myTodoCount;
}
