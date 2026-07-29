package com.zqk.house.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zqk.house.user.entity.User;
import com.zqk.house.user.entity.UserForm;
import com.zqk.house.user.mapper.UserMapper;
import com.zqk.house.util.DateUtil;
import com.zqk.house.util.PageResult;

import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 基础CRUD操作
    public boolean addUser(User user) {
        return userMapper.insert(user) > 0;
    }
    
    public boolean updateUser(User user) {
        // 根据ID获取原有用户信息
        User oldUser = userMapper.selectById(user.getId());
        if (oldUser == null) {
            return false;
        }
        return userMapper.update(user) > 0;
    }
    
    public boolean deleteUser(Long id) {
        return userMapper.deleteByPrimaryKey(id) > 0;
    }
    
    public User getUserByDocNumber(String docNumber) {
        return userMapper.selectByDocNumber(docNumber);
    }
    
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    // 查询相关
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }
    
    /**
     * 分页查询用户列表
     */
    public PageResult<User> getUserList(UserForm userForm) {
        // 处理日期，去除时分秒部分
        if (userForm != null) {
            userForm.setLeaseStartDate(DateUtil.toDateOnly(userForm.getLeaseStartDate()));
            userForm.setLeaseEndDate(DateUtil.toDateOnly(userForm.getLeaseEndDate()));
        }
        
        List<User> list = userMapper.getUserList(userForm);
        Long total = userMapper.getTotal(userForm);
        return new PageResult<>(list, total);
    }
    
    // 认证相关
    public User getUserByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }
} 