package com.zqk.house.flowdata.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 按人员展示：人员提交汇总（联表 sys_user + flow_form_record 聚合）
 */
@Data
public class PersonSubmitVO {
    /** 人员ID */
    private Long userId;
    private String userName;
    private String userEmpNo;
    private String userDept;
    /** 累计提交次数 */
    private Long submitCount;
    /** 参与期次数（按任务组去重） */
    private Long periodCount;
    /** 最近提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastSubmitTime;
}
