package com.zqk.house.flowtask.vo;

import com.zqk.house.flowdata.entity.FlowFormData;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import com.zqk.house.flowtemplate.entity.FlowTemplateNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
    /** 模板级字段配置（node_id 为空，不依附节点，如规章制度/采购说明） */
    private List<FlowTemplateField> templateFields;
    /** 模板级字段值（fieldId → value，创建人下发时赋值） */
    private Map<Long, String> templateData;
    /** 处理人填写的任务基础字段配置（fieldRole=2，不依附节点，处理人处理时填写） */
    private List<FlowTemplateField> handlerBaseFields;
    /** 当前节点当前处理人最近一次提交的处理人任务基础字段值（退回重做回填） */
    private Map<Long, String> currentBaseData;
    /** 处理人填写的任务基础字段汇总值（fieldId → value，取各绑定节点最新一次提交，随任务流转同步展示） */
    private Map<Long, String> handlerBaseData;
}
