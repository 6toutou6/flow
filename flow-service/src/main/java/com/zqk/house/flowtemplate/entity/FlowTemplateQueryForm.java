package com.zqk.house.flowtemplate.entity;

import lombok.Data;

@Data
public class FlowTemplateQueryForm {
    private Integer page;
    private Integer limit;
    private String templateName;
    private String category;
    private Integer status;
}
