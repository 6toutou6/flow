package com.zqk.house.flowtask.vo;

import lombok.Data;

/**
 * 成员任务的一个流程节点步骤（供成员卡片横向展示）。
 * status: done=已通过 current=处理中 pending=未到
 */
@Data
public class MemberNodeStepVO {

    private Integer stepNo;
    private Long nodeId;
    private String nodeName;
    private String status;
}
