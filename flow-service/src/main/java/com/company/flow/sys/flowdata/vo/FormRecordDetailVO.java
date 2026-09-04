package com.company.flow.sys.flowdata.vo;

import com.company.flow.sys.flowdata.entity.FlowFormData;
import com.company.flow.sys.flowtask.vo.FormDataItemVO;
import lombok.Data;

import java.util.List;

@Data
public class FormRecordDetailVO {
    private FormRecordListVO record;
    private List<FlowFormData> formDataList;
    /** 带字段标签/类型的表单数据展示项（label/type 快照优先，降级当前模板） */
    private List<FormDataItemVO> formDataItems;
}
