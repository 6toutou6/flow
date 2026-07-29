package com.zqk.house.room.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zqk.house.room.entity.Room;
import com.zqk.house.room.entity.RoomForm;
import com.zqk.house.room.service.RoomService;
import com.zqk.house.user.entity.User;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.Result;

import java.util.List;

@RestController
@RequestMapping("/room")
@CrossOrigin
public class RoomController {
    
    @Autowired
    private RoomService roomService;
    
    // 基础CRUD操作
    @PostMapping("/add")
    public Result<Void> addRoom(@RequestBody Room room) {
        return roomService.addRoom(room) ? 
            Result.success("添加成功") : 
            Result.fail("添加失败");
    }
    
    @PutMapping("/update")
    public Result<Void> updateRoom(@RequestBody Room room) {
        return roomService.updateRoom(room) ? 
            Result.success("更新成功") : 
            Result.fail("更新失败");
    }
    
    @DeleteMapping("/delete/{roomNumber}")
    public Result<Void> deleteRoom(@PathVariable String roomNumber) {
        return roomService.deleteRoom(roomNumber) ? 
            Result.success("删除成功") : 
            Result.fail("删除失败");
    }
    
    @GetMapping("/{roomNumber}")
    public Result<Room> getRoomByNumber(@PathVariable String roomNumber) {
        Room room = roomService.getRoomByNumber(roomNumber);
        return room != null ? 
            Result.success("获取成功", room) : 
            Result.notFound("房间不存在");
    }
    
    // 查询相关
    @PostMapping("/list")
    public Result<PageResult<Room>> getRoomList(@RequestBody RoomForm roomForm) {
        PageResult<Room> pageResult = roomService.getRoomList(roomForm);
        return Result.success("获取成功", pageResult);
    }

    @GetMapping("/history/{roomNumber}")
    public Result<List<User>> getRoomHistory(@PathVariable String roomNumber) {
        List<User> historyUsers = roomService.getRoomHistory(roomNumber);
        return Result.success("获取成功", historyUsers);
    }

    @GetMapping("/tenant/{roomNumber}")
    public Result<List<User>> getCurrentTenant(@PathVariable String roomNumber) {
        List<User> tenants = roomService.getCurrentTenant(roomNumber);
        if (tenants == null || tenants.isEmpty()) {
            return Result.notFound("该房间暂无租客");
        }
        // 出于安全考虑，不返回密码
        tenants.forEach(tenant -> tenant.setPassword(null));
        return Result.success("获取成功", tenants);
    }
} 