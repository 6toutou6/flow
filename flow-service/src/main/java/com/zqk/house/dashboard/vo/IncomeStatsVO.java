package com.zqk.house.dashboard.vo;

import lombok.Data;
import java.util.List;

@Data
public class IncomeStatsVO {
    private String batchStr;        // 批次号字符串
    private String incomeStr;       // 收入字符串
    private List<String> batchs;    // 批次号数组
    private List<Double> incomes;   // 收入数组，改为Double类型
    private Integer paymentCount;   // 支付笔数
} 