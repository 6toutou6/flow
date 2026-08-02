package com.zqk.house.flowdata.vo;

import com.zqk.house.flowdata.entity.FlowAttachment;
import com.zqk.house.flowdata.entity.FlowFormData;
import lombok.Data;

import java.util.List;

@Data
public class FormRecordDetailVO {
    private FormRecordListVO record;
    private List<FlowFormData> formDataList;
    private List<FlowAttachment> attachments;
}
