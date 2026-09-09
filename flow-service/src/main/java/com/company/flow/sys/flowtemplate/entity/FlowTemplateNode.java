package com.company.flow.sys.flowtemplate.entity;

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

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String templateId;
    private String nodeName;
    private Integer sortNum;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    private String nodeTips;
    /** 节点填写说明（创建人配置，展示给处理人如何填写） */
    private String guideText;
    /** 节点说明文件 JSON [{"name":"","url":""}] */
    private String guideFiles;
    /** 下一步处理人提示（创建人配置，提交节点选择下一处理人时展示） */
    private String nextHandlerTip;
    /** 条件分支配置 JSON {"branches":[...],"defaultNodeId":""}；为空则按 sort_num 顺序流转 */
    private String branchConfig;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
