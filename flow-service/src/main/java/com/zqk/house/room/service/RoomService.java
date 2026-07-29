package com.zqk.house.room.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zqk.house.room.entity.Room;
import com.zqk.house.room.entity.RoomForm;
import com.zqk.house.room.mapper.RoomMapper;
import com.zqk.house.user.entity.User;
import com.zqk.house.user.mapper.UserMapper;
import com.zqk.house.util.PageResult;

import java.util.List;

@Service
public class RoomService {
    
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    
    @Autowired
    private RoomMapper roomMapper;
    
    @Autowired
    private UserMapper userMapper;  // 注入UserMapper
    
    // 基础CRUD操作
    public boolean addRoom(Room room) {
        return roomMapper.insert(room) > 0;
    }
    
    public boolean updateRoom(Room room) {
        return roomMapper.update(room) > 0;
    }
    
    public boolean deleteRoom(String roomNumber) {
        return roomMapper.deleteByPrimaryKey(roomNumber) > 0;
    }
    
    public Room getRoomByNumber(String roomNumber) {
        return roomMapper.selectByPrimaryKey(roomNumber);
    }
    
    // 查询相关
    public List<Room> getAllRooms() {
        return roomMapper.selectAll();
    }
    
    /**
     * 分页查询房间列表
     */
    public PageResult<Room> getRoomList(RoomForm roomForm) {
        List<Room> list = roomMapper.getRoomList(roomForm);
        Long total = roomMapper.getTotal(roomForm);
        return new PageResult<>(list, total);
    }

    /**
     * 获取房间的历史住客
     */
    public List<User> getRoomHistory(String roomNumber) {
        return userMapper.getRoomHistory(roomNumber);
    }

    /**
     * 获取房间当前租客信息
     */
    public List<User> getCurrentTenant(String roomNumber) {
        log.info("查询房间 {} 的当前租客", roomNumber);
        List<User> tenants = roomMapper.getCurrentTenant(roomNumber);
        log.info("查询结果: {}", tenants);
        return tenants;
    }
} 