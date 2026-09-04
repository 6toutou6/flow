package com.company.flow.sys.base.autuser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.entity.UserQueryForm;
import com.company.flow.sys.base.autuser.mapper.UserMapper;
import com.company.flow.sys.base.autuser.service.UserService;
import com.company.flow.sys.base.autuser.vo.SysUserStatsVO;
import com.company.flow.sys.base.result.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String yyytId, String password) {
        User dbUser = userMapper.selectByYyytIdWithPassword(yyytId);
        if (dbUser == null || !password.equals(dbUser.getPassword())) {
            return null;
        }
        return dbUser;
    }

    @Override
    public List<User> listAllActive() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByAsc(User::getYyytId));
    }

    @Override
    public List<User> search(String userName, List<Integer> types) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        if (StringUtils.hasText(userName)) {
            wrapper.and(w -> w.like(User::getUserName, userName).or().like(User::getYyytId, userName));
        }
        if (types != null && !types.isEmpty()) {
            wrapper.in(User::getType, types);
        }
        wrapper.orderByAsc(User::getYyytId);
        return userMapper.selectList(wrapper);
    }

    @Override
    public PageResult<User> getPage(UserQueryForm form) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(form.getYyytId()), User::getYyytId, form.getYyytId())
               .like(StringUtils.hasText(form.getUserName()), User::getUserName, form.getUserName())
               .like(StringUtils.hasText(form.getDeptName()), User::getDeptName, form.getDeptName())
               .eq(form.getStatus() != null, User::getStatus, form.getStatus())
               .orderByAsc(User::getYyytId);
        Page<User> p = new Page<>(form.getPage() == null ? 1 : form.getPage(),
                form.getLimit() == null ? 10 : form.getLimit());
        Page<User> result = userMapper.selectPage(p, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    @Override
    public SysUserStatsVO stats() {
        SysUserStatsVO vo = new SysUserStatsVO();
        vo.setTotal(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        vo.setNormalCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));
        vo.setDisabledCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 0)));
        vo.setDeptCount(userMapper.countDistinctDept());
        return vo;
    }

    @Override
    public boolean addUser(User user) {
        return userMapper.insert(user) > 0;
    }

    @Override
    public boolean updateUser(User user) {
        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean deleteUser(String yyytId) {
        return userMapper.deleteById(yyytId) > 0;
    }

    @Override
    public User findByYyytId(String yyytId) {
        User user = userMapper.selectByYyytIdWithPassword(yyytId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
