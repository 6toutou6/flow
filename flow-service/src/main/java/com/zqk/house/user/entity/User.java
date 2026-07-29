package com.zqk.house.user.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * @author lzp
 * @Description: 用户实体类
 */
@Data
public class User {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 身份证号码
     */
    private String docNumber;

    /**
     * 电话号码
     */
    private String phoneNumber;

    /**
     * 职业
     */
    private String job;

    /**
     * 工作地址
     */
    private String workAddress;

    /**
     * 房间号码
     */
    private String roomNumber;

    /**
     * 租赁开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date leaseStartDate;

    /**
     * 租赁结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date leaseEndDate;

    /**
     * 租赁状态：在租/已退租
     */
    private String leaseStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
