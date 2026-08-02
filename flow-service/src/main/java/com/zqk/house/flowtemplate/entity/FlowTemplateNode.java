package com.zqk.house.flowtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 流程模板节点（对应 flow.flow_template_node 表）
 * 存储模板的有序节点链定义：下发 → 需求设计 → ... → 结束
 */
@Data
@TableName("flow_template_node")
public class FlowTemplateNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    private String nodeName;
    private Integer sortNum;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    private String nodeTips;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
