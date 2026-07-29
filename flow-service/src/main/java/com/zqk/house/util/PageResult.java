package com.zqk.house.util;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;    // �����б�
    private Long total;         // �ܼ�¼��
    
    public PageResult(List<T> records, Long total) {
        this.records = records;
        this.total = total;
    }
} 