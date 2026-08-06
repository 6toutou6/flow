package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 生成期次结果：期次ID + 期次名称 + 下次自动下发时间（供前端提示用户）。
 */
@Data
public class PeriodGenerateVO {
    /** 新期次ID */
    private Long periodId;
    /** 期次名称 */
    private String periodName;
    /** 下次自动下发时间（下一个未下发的周期开始时间；单次下发/手动临时期次为空） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date nextDispatchTime;
}
