package com.zqk.house.flowdata.vo;

import lombok.Data;

import java.util.List;

/**
 * 数据展示页聚合数据（统计卡 + 各图表）
 */
@Data
public class DashboardVO {
    /** 统计卡 */
    private DataStatsVO stats;
    /** 累计提交数 */
    private Long submitTotal;
    /** 参与人员数 */
    private Long personCount;
    /** 任务状态分布 */
    private List<NameCountVO> taskStatus;
    /** 期次状态分布 */
    private List<NameCountVO> periodStatus;
    /** 节点状态分布（待处理/已完成） */
    private List<NameCountVO> nodeStatus;
    /** 下发周期类型分布（每周/每月/每季度/单次） */
    private List<NameCountVO> periodCycle;
    /** 下发部门排行 */
    private List<NameCountVO> dispatchDeptRank;
    /** 处理人部门排行 */
    private List<NameCountVO> handlerDeptRank;
    /** 模板节点数分布（1个节点/2个节点/…各有多少模板） */
    private List<NameCountVO> templateNodeDist;
    /** 字段类型使用排行 */
    private List<NameCountVO> fieldTypeRank;
    /** 提交趋势（按粒度：日/月/季度/年） */
    private List<TrendPointVO> trend;
}
