package com.zqk.house.room.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 房间实体类
 */
@Data
public class Room {
    /**
     * 房间号码
     */
    private String roomNumber;

    /**
     * 房间类型
     */
    private String roomType;

    /**
     * 租金
     */
    private String rentPrice;

    /**
     * 押金
     */
    private String depositPrice;

    /**
     * 水费（单位：立方米）
     */
    private String waterPrice;

    /**
     * 电费（单位：千瓦时）
     */
    private String electricityPrice;

    /**
     * 房间面积（单位：平方米）
     */
    private String area;

    /**
     * 房间状态（已租/空置/维修中）
     */
    private String roomStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
} 