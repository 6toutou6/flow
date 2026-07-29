package com.zqk.house.rentpayment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zqk.house.rentpayment.entity.RentPayment;
import com.zqk.house.rentpayment.entity.RentPaymentForm;
import com.zqk.house.rentpayment.service.RentPaymentService;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.Result;

import java.util.List;

@RestController
@RequestMapping("/payment")
@CrossOrigin
public class RentPaymentController {
    
    @Autowired
    private RentPaymentService rentPaymentService;
    
    // 基础CRUD操作
    @PostMapping("/add")
    public Result<Void> addPayment(@RequestBody RentPayment payment) {
        return rentPaymentService.addPayment(payment) ? 
            Result.success("添加成功") : 
            Result.fail("添加失败");
    }
    
    @PutMapping("/update")
    public Result<Void> updatePayment(@RequestBody RentPayment payment) {
        return rentPaymentService.updatePayment(payment) ? 
            Result.success("更新成功") : 
            Result.fail("更新失败");
    }
    
    @DeleteMapping("/delete/{paymentId}")
    public Result<Void> deletePayment(@PathVariable String paymentId) {
        return rentPaymentService.deletePayment(paymentId) ? 
            Result.success("删除成功") : 
            Result.fail("删除失败");
    }
    
    @GetMapping("/{paymentId}")
    public Result<RentPayment> getPaymentById(@PathVariable String paymentId) {
        RentPayment payment = rentPaymentService.getPaymentById(paymentId);
        return payment != null ? 
            Result.success("获取成功", payment) : 
            Result.notFound("支付记录不存在");
    }
    
    // 查询相关
    @PostMapping("/list")
    public Result<PageResult<RentPayment>> getPaymentList(@RequestBody RentPaymentForm rentPaymentForm) {
        PageResult<RentPayment> pageResult = rentPaymentService.getPaymentList(rentPaymentForm);
        return Result.success("获取成功", pageResult);
    }
    
    @GetMapping("/room/{roomAmount}")
    public Result<List<RentPayment>> getPaymentsByRoom(@PathVariable String roomAmount) {
        List<RentPayment> payments = rentPaymentService.getPaymentsByRoom(roomAmount);
        return Result.success("获取成功", payments);
    }
    
    @GetMapping("/user/{userName}")
    public Result<List<RentPayment>> getPaymentsByUser(@PathVariable String userName) {
        List<RentPayment> payments = rentPaymentService.getPaymentsByUser(userName);
        return Result.success("获取成功", payments);
    }
} 