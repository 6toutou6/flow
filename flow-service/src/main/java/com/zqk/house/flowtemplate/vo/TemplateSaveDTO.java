package com.zqk.house.flowtemplate.vo;

import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import lombok.Data;

import java.util.List;

/**
 * 表单设计器保存：模板ID + 字段列表
 */
@Data
public class TemplateSaveDTO {
    private Long templateId;
    private List<FlowTemplateField> fields;
}
