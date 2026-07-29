package com.zqk.house.rentpayment.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 支付信息实体类
 */
@Data
public class RentPayment {
    /**
     * 支付ID
     */
    private String paymentId;

    /**
     * 付款人
     */
    private String userName;

    /**
     * 身份号码
     */
    private String docNumber;

    /**
     * 房间号
     */
    private String roomAmount;

    /**
     * 房屋金额
     */
    private String rentaMount;

    /**
     * 水费金额
     */
    private String waterBill;

    /**
     * 电费金额
     */
    private String electricityBill;

    /**
     * 支付日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date paymentDate;

    /**
     * 缴费批次
     */
    private String batch;

    /**
     * 支付状态（已支付/未支付）
     */
    private String paymentStatus;

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

    /**
     * 缴费总和
     */
    private String totalAmount;
} 