package com.company.flow.sys.base.attach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 附件表（对应 flow.attach 表）
 * 字段按业务要求固定，不许减少或变更：
 * attachId / bizId / fileName / ecsUrl / creator / createTime / modifiedTime
 * ecsUrl 为随机字符记录（无真实存储，预览/下载/删除均以日志输出体现）
 */
@Data
@TableName("attach")
public class Attach {

    /** 附件id：AT + 时间戳字符串 */
    @TableId(type = IdType.INPUT)
    private String attachId;

    /** 业务id（如任务id、表单记录id） */
    private String bizId;

    /** 文件名 */
    private String fileName;

    /** ecs地址（随机字符记录） */
    private String ecsUrl;

    /** 创建人 */
    private String creator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date modifiedTime;
}
