package com.zqk.house.sysuser.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zqk.house.sysuser.entity.SysUser;
import com.zqk.house.sysuser.mapper.SysUserMapper;
import com.zqk.house.user.vo.LoginResponse;
import com.zqk.house.util.JwtUtil;
import com.zqk.house.util.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    public PageResult<SysUser> getPage(Integer page, Integer limit,
                                       String username, String empNo,
                                       String realName, String deptName,
                                       Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
               .like(StringUtils.hasText(empNo), SysUser::getEmpNo, empNo)
               .like(StringUtils.hasText(realName), SysUser::getRealName, realName)
               .like(StringUtils.hasText(deptName), SysUser::getDeptName, deptName)
               .eq(status != null, SysUser::getStatus, status)
               .orderByDesc(SysUser::getId);

        Page<SysUser> p = new Page<>(page == null ? 1 : page, limit == null ? 10 : limit);
        Page<SysUser> result = sysUserMapper.selectPage(p, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    public boolean addUser(SysUser user) {
        return sysUserMapper.insert(user) > 0;
    }

    public boolean updateUser(SysUser user) {
        return sysUserMapper.updateById(user) > 0;
    }

    public boolean deleteUser(Long id) {
        return sysUserMapper.deleteById(id) > 0;
    }

    public SysUser getUserById(Long id) {
        return sysUserMapper.selectById(id);
    }

    /**
     * 登录校验：用户名+密码，成功返回含 token 的 LoginResponse
     */
    public LoginResponse login(String username, String password) {
        SysUser dbUser = sysUserMapper.selectByUsernameWithPassword(username);
        if (dbUser == null || !password.equals(dbUser.getPassword())) {
            return null;
        }
        String token = jwtUtil.generateToken(username);
        dbUser.setPassword(null);
        return new LoginResponse("Bearer " + token, dbUser);
    }

    /**
     * 按用户名查询（不含密码），用于 info 接口
     */
    public SysUser findByUsername(String username) {
        SysUser user = sysUserMapper.selectByUsernameWithPassword(username);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
