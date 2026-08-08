package com.zqk.house.flowtask.vo;

import lombok.Data;

/**
 * 任务管理页统计卡数据（部门可见性过滤后）
 */
@Data
public class DispatchStatsVO {
    /** 任务总数 */
    private Long taskCount;
    /** 启用中任务数 */
    private Long activeCount;
    /** 已下发期次数 */
    private Long periodCount;
    /** 参与人员数（任务配置人员累计） */
    private Long memberCount;
}
