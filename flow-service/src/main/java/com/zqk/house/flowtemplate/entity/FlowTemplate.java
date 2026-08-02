package com.zqk.house.flowtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 流程模板（对应 flow.flow_template 表）
 */
@Data
@TableName("flow_template")
public class FlowTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateName;
    private String category;
    private Integer version;
    /** 0停用 1启用 */
    private Integer status;
    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
