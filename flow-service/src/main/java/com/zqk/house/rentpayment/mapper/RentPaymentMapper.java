package com.zqk.house.rentpayment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zqk.house.dashboard.vo.IncomeStatsVO;
import com.zqk.house.dashboard.vo.PaymentStatsVO;
import com.zqk.house.dashboard.vo.UnpaidRoomVO;
import com.zqk.house.rentpayment.entity.RentPayment;
import com.zqk.house.rentpayment.entity.RentPaymentForm;
import com.zqk.house.util.BaseMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface RentPaymentMapper extends BaseMapper<RentPayment, String> {
    
    // 分页查询
    List<RentPayment> getRentPaymentList(RentPaymentForm rentPaymentForm);
    Long getTotal(RentPaymentForm rentPaymentForm);
    
    // 根据房间号查询支付记录
    List<RentPayment> getPaymentsByRoom(String roomAmount);
    
    // 根据用户查询支付记录
    List<RentPayment> getPaymentsByUser(String userName);

    Integer getCurrentMonthIncome(String currentMonth);
    Integer getPaidCount(String currentMonth);
    Integer getUnpaidCount(String currentMonth);
    Integer getMonthIncome(String batch);

    // 按日期范围统计收入
    Integer getIncomeByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // 按日期范围统计已支付数量
    Integer getPaidCountByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // 按日期范围统计未支付数量
    Integer getUnpaidCountByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // 按日期范围获取月度收入统计
    List<Map<String, Object>> getMonthlyIncomeByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // 查询日期范围内未支付的房间记录
    List<UnpaidRoomVO> getUnpaidRooms(@Param("startBatch") String startBatch, @Param("endBatch") String endBatch);

    /**
     * 获取支付统计信息
     */
    PaymentStatsVO getPaymentStats(@Param("startBatch") String startBatch, @Param("endBatch") String endBatch);

    /**
     * 获取收入统计信息
     */
    IncomeStatsVO getIncomeStats(@Param("startBatch") String startBatch, @Param("endBatch") String endBatch);
} 