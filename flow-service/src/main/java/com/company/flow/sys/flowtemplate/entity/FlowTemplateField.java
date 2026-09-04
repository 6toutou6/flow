package com.company.flow.sys.flowtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String templateId;
    /** 归属节点ID flow_template_node.id */
    private String nodeId;
    /** 填写方式 1=创建人填写（下发时） 2=处理人填写（流程中填写，nodeId 为空时生效） */
    private Integer fieldRole;
    /** 处理人填写字段的绑定节点ID（fieldRole=2 时生效：仅处理该节点时需填写） */
    private String bindNodeId;
    /** 前端设计器保存用的绑定节点索引（nodes 列表下标，非数据库列，保存时转换为新节点ID） */
    @TableField(exist = false)
    private Integer bindNodeIndex;
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
