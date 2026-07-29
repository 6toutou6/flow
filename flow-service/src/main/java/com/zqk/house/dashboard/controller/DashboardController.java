package com.zqk.house.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zqk.house.dashboard.service.DashboardService;
import com.zqk.house.dashboard.vo.IncomeStatsVO;
import com.zqk.house.dashboard.vo.PaymentStatsVO;
import com.zqk.house.dashboard.vo.RoomStatsVO;
import com.zqk.house.dashboard.vo.UnpaidRoomVO;
import com.zqk.house.rentpayment.mapper.RentPaymentMapper;
import com.zqk.house.util.Result;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private RentPaymentMapper rentPaymentMapper;
    
    @GetMapping("/roomStats")
    public Result<RoomStatsVO> getRoomStats() {
        return Result.success("获取成功", dashboardService.getRoomStats());
    }
    
    /**
     * 获取支付统计信息
     * @param startBatch 开始批次，格式：202401
     * @param endBatch 结束批次，格式：202403
     */
    @GetMapping("/paymentStats")
    public Result<PaymentStatsVO> getPaymentStats(
            @RequestParam(required = false) String startBatch,
            @RequestParam(required = false) String endBatch) {
        return Result.success("获取成功", dashboardService.getPaymentStats(startBatch, endBatch));
    }
    
    /**
     * 获取收入统计信息
     * @param startBatch 开始批次，格式：202401
     * @param endBatch 结束批次，格式：202403
     */
    @GetMapping("/incomeStats")
    public Result<IncomeStatsVO> getIncomeStats(
            @RequestParam(required = false) String startBatch,
            @RequestParam(required = false) String endBatch) {
        return Result.success("获取成功", dashboardService.getIncomeStats(startBatch, endBatch));
    }
    
    /**
     * 获取未支付房间列表
     * @param startBatch 开始批次，格式：202401
     * @param endBatch 结束批次，格式：202403
     */
    @GetMapping("/unpaidRooms")
    public Result<List<UnpaidRoomVO>> getUnpaidRooms(
            @RequestParam String startBatch,
            @RequestParam String endBatch) {
        List<UnpaidRoomVO> unpaidRooms = rentPaymentMapper.getUnpaidRooms(startBatch, endBatch);
        return Result.success("获取成功", unpaidRooms);
    }
} 