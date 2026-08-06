package com.zqk.house.flowtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 模板版本记录（对应 flow.flow_template_version 表）
 * 保存为新版本时，将旧配置（节点+字段 JSON 快照）归档，供版本记录查看
 */
@Data
@TableName("flow_template_version")
public class FlowTemplateVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    private Integer version;
    private String versionDesc;
    private Long modifierId;
    private String modifierName;
    /** 模板配置快照 JSON（nodes + templateFields） */
    private String configSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
