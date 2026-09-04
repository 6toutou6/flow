package com.company.flow.sys.flowtemplate.vo;

import com.company.flow.sys.flowtemplate.entity.FlowTemplateField;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateNode;
import lombok.Data;

import java.util.List;

/**
 * 流程设计器保存入参：模板ID + 节点链（每个节点含字段）+ 模板级字段（不依附节点）
 * 替代旧的 TemplateSaveDTO
 */
@Data
public class TemplateFlowSaveDTO {
    private String templateId;
    /** 保存方式：current=保存到当前版本（覆盖，版本号不变） new=保存为新版本（版本号+1，旧配置归档） */
    private String saveMode;
    /** 改动说明（保存版本时用户填写，写入模板/版本记录） */
    private String versionDesc;
    /** 模板级字段（node_id 为空，如规章制度/采购说明等任务基础信息） */
    private List<FlowTemplateField> templateFields;
    private List<NodeItem> nodes;

    @Data
    public static class NodeItem {
        private FlowTemplateNode node;
        private List<FlowTemplateField> fields;
    }
}
