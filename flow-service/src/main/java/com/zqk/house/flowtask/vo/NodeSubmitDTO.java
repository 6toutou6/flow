package com.zqk.house.flowtask.vo;

import com.zqk.house.flowdata.entity.FlowFormData;
import lombok.Data;

import java.util.List;

/**
 * 节点提交入参（任务流转核心）
 * 处理人填写当前节点表单后提交，指定下一节点处理人
 */
@Data
public class NodeSubmitDTO {
    /** 任务ID */
    private Long taskId;
    /** 当前任务流转节点ID flow_task_node.id */
    private Long taskNodeId;
    /** 表单数据（file/image 类型字段值存文件名文本） */
    private List<FlowFormData> formData;
    /** 下一节点处理人ID列表（多选，每人一个独立分支；通过且非结束节点必填） */
    private List<Long> nextHandlerIds;
    /** 下一节点处理人ID（单值，已废弃，向后兼容保留；submit 优先读 nextHandlerIds） */
    private Long nextHandlerUserId;
    /** 退回目标模板节点ID flow_template_node.id（action=reject 时必填，须为已到达过的节点） */
    private Long rejectToNodeId;
    /** 退回原因（action=reject 时必填） */
    private String rejectReason;
    /** 通过意见（action=pass 时填写，非必填） */
    private String passComment;
    /** 操作类型 pass=通过(默认) reject=退回到指定节点 */
    private String action;
}
