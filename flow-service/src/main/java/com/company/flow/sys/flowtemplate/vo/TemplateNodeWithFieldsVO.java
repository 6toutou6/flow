package com.company.flow.sys.flowtemplate.vo;

import com.company.flow.sys.flowtemplate.entity.FlowTemplateField;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateNode;
import lombok.Data;

import java.util.List;

/**
 * 节点 + 其字段列表（流程设计器详情树的一个节点）
 */
@Data
public class TemplateNodeWithFieldsVO {
    private FlowTemplateNode node;
    private List<FlowTemplateField> fields;
}
