package com.company.flow.sys.base.result;

import lombok.Data;

import java.util.List;

/**
 * 统一分页返回结构
 */
@Data
public class PageResult<T> {
    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private Long total;

    public PageResult(List<T> records, Long total) {
        this.records = records;
        this.total = total;
    }
}
