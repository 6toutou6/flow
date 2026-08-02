package com.zqk.house.flowtask.entity;

import lombok.Data;

@Data
public class FlowTaskQueryForm {
    private Integer page;
    private Integer limit;
    private String taskName;
    private Integer status;
}
