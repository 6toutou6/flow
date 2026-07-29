package com.zqk.house.dashboard.vo;

import lombok.Data;

@Data
public class UnpaidRoomVO {
    private String roomAmount;     // 房间号
    private String userName;       // 用户名
    private String batch;          // 未支付批次
    private String totalAmount;    // 应付金额
} 