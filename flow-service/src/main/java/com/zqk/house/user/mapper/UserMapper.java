package com.zqk.house.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.zqk.house.user.entity.User;
import com.zqk.house.user.entity.UserForm;
import com.zqk.house.util.BaseMapper;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User, Long> {
    // docNumber作为主键类型使用String
    // 可以添加特定的用户查询方法
    
    int deleteByDocNumber(String docNumber);
    
    User selectByDocNumber(String docNumber);
    
    User selectById(Long id);
    
    // 查询所有
    List<User> selectAll();
    
    List<User> getUserList(UserForm userForm);
    
    Long getTotal(UserForm userForm);
    
    // 认证相关
    User getUserByUsername(String username);

    /**
     * 获取房间的历史住客
     */
    List<User> getRoomHistory(String roomNumber);
} 