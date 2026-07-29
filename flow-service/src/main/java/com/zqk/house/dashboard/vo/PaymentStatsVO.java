package com.zqk.house.dashboard.vo;

import lombok.Data;
import java.util.List;

@Data
public class PaymentStatsVO {
    private Integer currentMonthIncome;   // 本月收入
    private Integer paidCount;            // 已缴费人数
    private Integer unpaidCount;          // 未缴费人数
    private List<UnpaidRoomVO> unpaidRooms; // 未支付房间列表
}