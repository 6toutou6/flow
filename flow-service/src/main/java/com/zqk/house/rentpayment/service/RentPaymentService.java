package com.zqk.house.rentpayment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zqk.house.rentpayment.entity.RentPayment;
import com.zqk.house.rentpayment.entity.RentPaymentForm;
import com.zqk.house.rentpayment.mapper.RentPaymentMapper;
import com.zqk.house.util.PageResult;

import java.util.List;

@Service
public class RentPaymentService {
    
    @Autowired
    private RentPaymentMapper rentPaymentMapper;
    
    // 基础CRUD操作
    public boolean addPayment(RentPayment payment) {
        return rentPaymentMapper.insert(payment) > 0;
    }
    
    public boolean updatePayment(RentPayment payment) {
        return rentPaymentMapper.update(payment) > 0;
    }
    
    public boolean deletePayment(String paymentId) {
        return rentPaymentMapper.deleteByPrimaryKey(paymentId) > 0;
    }
    
    public RentPayment getPaymentById(String paymentId) {
        return rentPaymentMapper.selectByPrimaryKey(paymentId);
    }
    
    // 查询相关
    public List<RentPayment> getAllPayments() {
        return rentPaymentMapper.selectAll();
    }
    
    /**
     * 分页查询支付记录
     */
    public PageResult<RentPayment> getPaymentList(RentPaymentForm rentPaymentForm) {
        List<RentPayment> list = rentPaymentMapper.getRentPaymentList(rentPaymentForm);
        Long total = rentPaymentMapper.getTotal(rentPaymentForm);
        return new PageResult<>(list, total);
    }
    
    /**
     * 获取房间的支付记录
     */
    public List<RentPayment> getPaymentsByRoom(String roomAmount) {
        return rentPaymentMapper.getPaymentsByRoom(roomAmount);
    }
    
    /**
     * 获取用户的支付记录
     */
    public List<RentPayment> getPaymentsByUser(String userName) {
        return rentPaymentMapper.getPaymentsByUser(userName);
    }
} 