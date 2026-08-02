package com.zqk.house.flowdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 表单填报明细数据（对应 flow.flow_form_data 表）
 */
@Data
@TableName("flow_form_data")
public class FlowFormData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;
    private Long fieldId;
    private String fieldKey;
    private String fieldValue;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
