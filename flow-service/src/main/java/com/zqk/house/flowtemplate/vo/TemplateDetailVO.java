package com.zqk.house.flowtemplate.vo;

import com.zqk.house.flowtemplate.entity.FlowTemplate;
import lombok.Data;

import java.util.List;

/**
 * 模板详情：模板元数据 + 节点链（每个节点含其字段）
 */
@Data
public class TemplateDetailVO {
    private FlowTemplate template;
    private List<TemplateNodeWithFieldsVO> nodes;
}
