package com.zqk.house.flowtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 模板表单字段（对应 flow.flow_template_field 表）
 * enum_options 用 String 接收，前端 JSON.parse/stringify 处理
 */
@Data
@TableName("flow_template_field")
public class FlowTemplateField {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    /** 归属节点ID flow_template_node.id */
    private Long nodeId;
    private String fieldKey;
    private String fieldLabel;
    /** 字段类型：text/textarea/number/date/radio/checkbox/file/image */
    private String fieldType;
    private Integer sortNum;
    private Integer required;
    private String placeholder;
    private String fieldTips;
    private Integer maxLength;
    /** 枚举选项 JSON 字符串 [{"label":"","value":""}] */
    private String enumOptions;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
