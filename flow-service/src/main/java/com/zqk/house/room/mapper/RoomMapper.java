package com.zqk.house.room.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.zqk.house.room.entity.Room;
import com.zqk.house.room.entity.RoomForm;
import com.zqk.house.user.entity.User;
import com.zqk.house.util.BaseMapper;

import java.util.List;

@Mapper
public interface RoomMapper extends BaseMapper<Room, String> {
    
    // 分页查询
    List<Room> getRoomList(RoomForm roomForm);
    Long getTotal(RoomForm roomForm);
    Integer getTotalCount();
    Integer getCountByStatus(String status);

    /**
     * 获取房间当前租客信息
     */
    List<User> getCurrentTenant(String roomNumber);
} 