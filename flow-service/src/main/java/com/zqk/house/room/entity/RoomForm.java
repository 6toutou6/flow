package com.zqk.house.room.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间查询表单
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoomForm extends Room {
    /**
     * 当前页码，从1开始
     */
    private Integer pageIndex = 1;
    
    /**
     * 每页显示记录数
     */
    private Integer pageSize = 10;
    
    /**
     * 获取MySQL分页查询的起始位置
     */
    public Integer getOffset() {
        return (pageIndex - 1) * pageSize;
    }
} 