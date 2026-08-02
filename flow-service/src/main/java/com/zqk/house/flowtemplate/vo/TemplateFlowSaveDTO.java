package com.zqk.house.flowtemplate.vo;

import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import com.zqk.house.flowtemplate.entity.FlowTemplateNode;
import lombok.Data;

import java.util.List;

/**
 * 流程设计器保存入参：模板ID + 节点链（每个节点含字段）
 * 替代旧的 TemplateSaveDTO
 */
@Data
public class TemplateFlowSaveDTO {
    private Long templateId;
    private List<NodeItem> nodes;

    @Data
    public static class NodeItem {
        private FlowTemplateNode node;
        private List<FlowTemplateField> fields;
    }
}
