package com.zqk.house.dashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zqk.house.dashboard.vo.IncomeStatsVO;
import com.zqk.house.dashboard.vo.PaymentStatsVO;
import com.zqk.house.dashboard.vo.RoomStatsVO;
import com.zqk.house.rentpayment.mapper.RentPaymentMapper;
import com.zqk.house.room.mapper.RoomMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    @Autowired
    private RoomMapper roomMapper;
    
    @Autowired
    private RentPaymentMapper rentPaymentMapper;
    
    /**
     * 获取房间统计信息
     */
    public RoomStatsVO getRoomStats() {
        RoomStatsVO stats = new RoomStatsVO();
        stats.setTotalRooms(roomMapper.getTotalCount());
        stats.setRentedRooms(roomMapper.getCountByStatus("已租"));
        stats.setVacantRooms(roomMapper.getCountByStatus("空置"));
        stats.setMaintainRooms(roomMapper.getCountByStatus("维修中"));
        return stats;
    }
    
    /**
     * 获取支付统计信息
     * @param startBatch 开始批次，格式：202401
     * @param endBatch 结束批次，格式：202403
     */
    public PaymentStatsVO getPaymentStats(String startBatch, String endBatch) {
        return rentPaymentMapper.getPaymentStats(startBatch, endBatch);
    }
    
    /**
     * 获取收入统计信息
     * @param startBatch 开始批次，格式：202401
     * @param endBatch 结束批次，格式：202403
     */
    public IncomeStatsVO getIncomeStats(String startBatch, String endBatch) {
        IncomeStatsVO stats = rentPaymentMapper.getIncomeStats(startBatch, endBatch);
        if (stats != null && stats.getBatchStr() != null) {
            // 将逗号分隔的字符串转换为List
            List<String> batchList = Arrays.asList(stats.getBatchStr().split(","));
            List<Double> incomeList = Arrays.stream(stats.getIncomeStr().split(","))
                .map(Double::parseDouble)
                .collect(Collectors.toList());
            
            stats.setBatchs(batchList);
            stats.setIncomes(incomeList);
        }
        return stats;
    }
} 