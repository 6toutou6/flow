package com.zqk.house.flowdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 附件文件（对应 flow.flow_attachment 表）
 */
@Data
@TableName("flow_attachment")
public class FlowAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileSuffix;
    private Long uploadUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
