package com.company.flow.sys.flowtask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务交接申请（对应 flow.flow_task_handover 表）。
 * 处理人 A 把某任务配置下自己名下的「全部期次 + 在该任务下的全部节点席位（含已提交历史节点）」交接给 B；
 * 创建人同部门的部门管理员任一审批通过后生效：期次 owner_id 与节点处理人 A→B。
 */
@Data
@TableName("flow_task_handover")
public class FlowTaskHandover {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 任务配置ID（flow_dispatch.id） */
    private String dispatchId;
    /** 任务名称（冗余，列表展示） */
    private String dispatchName;
    /** 创建部门ID（任务配置 flow_dispatch.dept_id，不落库，列表按创建部门折叠用） */
    @TableField(exist = false)
    private Long deptId;
    /** 创建部门名称（不落库，列表展示用） */
    @TableField(exist = false)
    private String deptName;

    /** 交接人（原归属人 A） */
    private String fromUserId;
    private String fromUserName;
    /** 接手人 B */
    private String toUserId;
    private String toUserName;

    /** 是否同步任务配置名单 flow_task_member（0否 1是；勾选后未来期次下发给 B） */
    private Integer syncMember;
    /** 「同步名单」是否适用（交接人是否在任务配置名单中，不落库，仅列表判断用） */
    @TableField(exist = false)
    private Boolean syncMemberApplicable;
    /** 当前登录用户在该记录中的角色（交接人 / 接手人，均不是时为 null；不落库，仅列表展示用） */
    @TableField(exist = false)
    private String myRole;
    /** 0待审批 1已通过 2已拒绝 */
    private Integer status;
    /** 涉及我名下期次数（申请时统计，审批通过后仍可展示） */
    private Integer periodCount;
    /** 涉及我名下节点席位数（含已提交历史节点，申请时统计） */
    private Integer nodeCount;
    /** 交接说明 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date applyTime;

    /** 审批人 */
    private String approverId;
    private String approverName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approveTime;
    /** 拒绝原因 */
    private String rejectReason;
}
