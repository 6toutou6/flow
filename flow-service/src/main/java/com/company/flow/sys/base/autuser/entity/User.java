package com.company.flow.sys.base.autuser.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 系统用户实体（对应 flow.aut_user 表）
 * 用户标识统一为「用户号 yyyt_id + 中文姓名 user_name」，一切匹配/关联/通知用 yyyt_id
 */
@Data
@TableName("aut_user")
public class User {

    /** 用户号（主键，唯一标识） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String yyytId;

    /** 中文姓名（展示用） */
    private String userName;

    /** 所属部门 */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 用户类型：9综合室 0普通用户 */
    private Integer type;

    /** 密码（默认查询不返回，新增/更新时才写入） */
    @TableField(select = false)
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态 0禁用 1正常 */
    private Integer status;

    /** 是否超管（后端配置文件 system.admin.userIds 判定，非数据库字段，前端展示用） */
    @TableField(exist = false)
    private Boolean superAdmin;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
