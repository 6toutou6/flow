package com.zqk.house.flowtask.vo;

import lombok.Data;

/**
 * 我的任务-期次内的流程节点（完整节点链 + 当前用户在该节点的完成状态）
 */
@Data
public class TodoNodeVO {
    /** 模板节点ID flow_template_node.id */
    private Long nodeId;
    private String nodeName;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    /** 完成状态 0未开始 1已完成 2进行中（当前待处理节点） */
    private Integer status;
}
