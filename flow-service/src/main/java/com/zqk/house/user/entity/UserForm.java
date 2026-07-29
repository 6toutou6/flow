package com.zqk.house.user.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserForm extends User {
    
    /**
     * ��ǰҳ�룬��1��ʼ
     */
    private Integer pageIndex = 1;
    
    /**
     * ÿҳ��ʾ��¼��
     */
    private Integer pageSize = 10;
    
    /**
     * ��ȡMySQL��ҳ����ʼλ��
     */
    public Integer getOffset() {
        return (pageIndex - 1) * pageSize;
    }
} 