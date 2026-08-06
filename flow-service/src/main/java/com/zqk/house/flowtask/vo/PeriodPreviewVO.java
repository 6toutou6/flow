package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 期次预览/生成结果：根据任务下发配置（flow_dispatch_config）周期 + 是否立即下发，
 * 计算期次序号、默认期次名、开始/截止时间、期间标识与下次自动下发时间。
 * 截止日期口径：开始（触发日） + deadline_days 天；补发且已过期则顺延为当前 + deadline_days。
 */
@Data
public class PeriodPreviewVO {
    /** 期次序号（同一任务下递增） */
    private Integer periodNo;
    /** 默认期次名称（如 2026-08 / 2026年第33周 / 2026年第3季度），下发时用户可修改 */
    private String periodName;
    /** 期间标识（如 2026-Q3 / 2026-08 / 2026-W31），用于自动下发防重复检测 */
    private String periodKey;
    /** 期次开始时间（触发日） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    /** 期次截止时间（开始 + deadline_days；补发已过期则顺延为当前 + deadline_days） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    /** 截止前N天催办（任务配置，仅日志输出） */
    private Integer urgeDays;
    /** 当期次是否已下发（立即下发时检测，已下发则禁止重复下发） */
    private Boolean alreadyDispatched;
    /** 下次自动下发时间（生成当前期次后，下一个未下发的周期开始时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date nextDispatchTime;
}
