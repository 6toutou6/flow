package com.company.flow.sys.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 期次节点快照（对应 flow.flow_task_dispatch_node 表）
 * 下发期次时锁定当期模板节点全部配置（提示/填写说明/说明文件/下一步提示/字段定义），
 * 防模板后续升级影响历史期次；临时人员新增时也从该快照复制节点。
 */
@Data
@TableName("flow_task_dispatch_node")
public class FlowTaskDispatchNode {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 期次ID flow_task_dispatch.id */
    private String dispatchId;
    /** 来源模板节点ID flow_template_node.id（溯源） */
    private String nodeId;
    private String nodeName;
    private Integer sortNum;
    /** 节点类型 1开始 2中间 3结束 */
    private Integer nodeType;
    /** 节点提示（快照） */
    private String nodeTips;
    /** 节点填写说明（快照） */
    private String guideText;
    /** 说明文件 JSON（快照） */
    private String guideFiles;
    /** 下一步处理人提示（快照） */
    private String nextHandlerTip;
    /** 节点字段定义 JSON（快照，含绑定该节点的处理人字段 fieldRole=2） */
    private String fieldsJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
