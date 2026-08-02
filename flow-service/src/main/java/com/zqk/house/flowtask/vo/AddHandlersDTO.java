package com.zqk.house.flowtask.vo;

import lombok.Data;

import java.util.List;

/**
 * 任务临时增加处理人入参：任务ID + 处理人ID列表
 * 新处理人将加入任务当前进行中的节点，与现有处理人并行处理当前节点
 */
@Data
public class AddHandlersDTO {
    private Long taskId;
    /** 新增处理人ID列表 */
    private List<Long> handlerIds;
}
