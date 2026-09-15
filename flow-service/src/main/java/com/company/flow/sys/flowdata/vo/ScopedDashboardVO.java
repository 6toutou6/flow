package com.company.flow.sys.flowdata.vo;

import lombok.Data;

import java.util.List;

/**
 * 分维度（个人 / 部门）数据展示的聚合返回对象。
 * 个人维度只使用 stats / taskStatus / nodeStatus / trend；部门维度全部使用。
 */
@Data
public class ScopedDashboardVO {

    /** 统计卡（口径见 ScopedStatsVO） */
    private ScopedStatsVO stats;

    /** 任务状态分布 */
    private List<NameCountVO> taskStatus;

    /** 期次状态分布（仅部门维度） */
    private List<NameCountVO> periodStatus;

    /** 节点进度（待处理/已完成） */
    private List<NameCountVO> nodeStatus;

    /** 下发排行：本部门各管理员下发的期次数（仅部门维度） */
    private List<NameCountVO> dispatchRank;

    /** 下发期次中未处理的任务数，按期次列出（仅部门维度） */
    private List<NameCountVO> periodPendingRank;

    /** 字段类型使用排行（仅部门维度，按本部门任务所用模板统计） */
    private List<NameCountVO> fieldTypeRank;

    /** 模板节点数分布（仅部门维度，按本部门任务所用模板统计） */
    private List<NameCountVO> templateNodeDist;

    /** 提交趋势 */
    private List<TrendPointVO> trend;
}
