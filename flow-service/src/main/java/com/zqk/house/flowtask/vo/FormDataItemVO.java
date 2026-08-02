package com.zqk.house.flowtask.vo;

import lombok.Data;

/**
 * 表单数据展示项（历史表单查看用：字段标签 + 字段值）
 */
@Data
public class FormDataItemVO {
    /** 字段标签（中文展示名） */
    private String fieldLabel;
    /** 字段值 */
    private String fieldValue;
}
