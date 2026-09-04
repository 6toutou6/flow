package com.company.flow.sys.flowtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String templateName;
    private String category;
    private Integer version;
    /** 当前版本改动说明（保存版本时用户填写） */
    private String versionDesc;
    /** 0停用 1启用 */
    private String status;
    private String creatorId;
    /** 最近修改人ID */
    private String modifierId;
    /** 最近修改人姓名（冗余） */
    private String modifierName;
    /** 创建人部门ID（部门内可见；样例公共可见） */
    private Long deptId;
    /** 1=样例(公共可见不可改) 0=普通 */
    private Integer isSample;

    /** 创建人姓名（冗余，随 creatorId 一并入库） */
    private String creatorName;
    /** 创建人部门名称（列表展示，非数据库字段） */
    @TableField(exist = false)
    private String deptName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
