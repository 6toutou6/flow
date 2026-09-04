package com.company.flow.sys.flowtemplate.vo;

import com.company.flow.sys.flowtemplate.entity.FlowTemplate;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateField;
import lombok.Data;

import java.util.List;

/**
 * 模板详情：模板元数据 + 节点链（每个节点含其字段）+ 模板级字段（不依附节点）
 */
@Data
public class TemplateDetailVO {
    private FlowTemplate template;
    private List<TemplateNodeWithFieldsVO> nodes;
    /** 模板级字段（node_id 为空，如规章制度/采购说明等任务基础信息） */
    private List<FlowTemplateField> templateFields;
}
