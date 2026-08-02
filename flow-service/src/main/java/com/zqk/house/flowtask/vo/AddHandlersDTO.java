package com.zqk.house.flowtask.vo;

import lombok.Data;

import java.util.List;

/**
 * 新增人员入参：主任务ID + 处理人ID列表
 * 为每个新增处理人在该主任务下创建一条独立成员任务
 */
@Data
public class AddHandlersDTO {
    /** 主任务ID（下发批次） */
    private Long dispatchId;
    /** 新增处理人ID列表 */
    private List<Long> handlerIds;
}
