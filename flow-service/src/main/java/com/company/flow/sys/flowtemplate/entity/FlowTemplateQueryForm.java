package com.company.flow.sys.flowtemplate.entity;

import lombok.Data;

@Data
public class FlowTemplateQueryForm {
    private Integer page;
    private Integer limit;
    private String templateName;
    private String category;
    private String status;
}
