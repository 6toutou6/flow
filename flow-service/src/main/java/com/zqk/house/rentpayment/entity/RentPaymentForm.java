package com.zqk.house.rentpayment.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;

/**
 * 支付信息查询表单
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RentPaymentForm extends RentPayment {
    /**
     * 支付日期范围
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endDate;
    
    /**
     * 当前页码，从1开始
     */
    private Integer pageIndex = 1;
    
    /**
     * 每页显示记录数
     */
    private Integer pageSize = 10;
    
    /**
     * 获取MySQL分页查询的起始位置
     */
    public Integer getOffset() {
        return (pageIndex - 1) * pageSize;
    }
} 