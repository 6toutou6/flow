package com.zqk.house.util;

import java.util.List;

/**
 * 通用Mapper接口，定义基本的CRUD操作
 * @param <T> 实体类型
 * @param <K> 主键类型
 */
public interface BaseMapper<T, K> {
    
    /**
     * 根据主键查询
     */
    T selectByPrimaryKey(K key);
    
    /**
     * 查询所有记录
     */
    List<T> selectAll();
    
    /**
     * 插入记录
     */
    int insert(T entity);
    
    /**
     * 更新记录
     */
    int update(T entity);
    
    /**
     * 根据主键删除
     */
    int deleteByPrimaryKey(K key);
} 