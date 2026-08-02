package com.zqk.house.flowdata.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class FormRecordQueryForm {
    private Integer page;
    private Integer limit;
    private Long userId;
    private Long templateId;
    private Long taskId;
    private Integer recordStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endDate;
}
