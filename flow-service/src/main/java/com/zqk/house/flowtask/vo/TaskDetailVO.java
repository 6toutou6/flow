package com.zqk.house.flowtask.vo;

import com.zqk.house.flowdata.entity.FlowFormData;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import com.zqk.house.flowtemplate.entity.FlowTemplateNode;
import lombok.Data;

import java.util.List;

/**
 * 任务详情：任务 + 流转进度 + 当前节点字段配置（用于渲染处理表单）
 */
@Data
public class TaskDetailVO {
    private FlowTask task;
    /** 流转进度（全部节点） */
    private List<TaskProgressVO> taskNodes;
    /** 当前节点要填写的字段配置（仅当前处理人待办时有值） */
    private List<FlowTemplateField> currentNodeFields;
    /** 当前节点已填的表单数据（草稿/回显，可选） */
    private List<FlowFormData> currentFormData;
    /** 模板完整节点链（用于展示未到节点的灰色骨架） */
    private List<FlowTemplateNode> templateNodes;
}
