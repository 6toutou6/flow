package com.zqk.house.dashboard.vo;

import lombok.Data;

@Data
public class RoomStatsVO {
    private Integer totalRooms;      // 总房间数
    private Integer rentedRooms;     // 已租房间数
    private Integer vacantRooms;     // 空置房间数
    private Integer maintainRooms;   // 维修中房间数
} 