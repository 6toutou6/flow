package com.company.flow.sys.base.enums;

/**
 * 流程节点类型：开始 / 中间 / 结束
 */
public enum NodeType {

    START(1, "开始"),
    MIDDLE(2, "中间"),
    END(3, "结束");

    private final int code;
    private final String text;

    NodeType(int code, String text) {
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
