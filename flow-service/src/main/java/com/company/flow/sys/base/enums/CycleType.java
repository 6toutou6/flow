package com.company.flow.sys.base.enums;

/**
 * 下发周期类型：每周 / 每月 / 每季度 / 单次下发
 */
public enum CycleType {

    WEEK(1, "每周"),
    MONTH(2, "每月"),
    QUARTER(3, "每季度"),
    ONCE(4, "单次下发");

    private final int code;
    private final String text;

    CycleType(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
}
