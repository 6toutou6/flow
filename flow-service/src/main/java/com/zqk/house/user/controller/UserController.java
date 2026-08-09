package com.zqk.house.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.zqk.house.user.vo.LoginResponse;
import com.zqk.house.sysuser.entity.SysUser;
import com.zqk.house.sysuser.service.SysUserService;
import com.zqk.house.util.JwtUtil;
import com.zqk.house.util.Result;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    // 认证相关：基于 sys_user 表
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody SysUser user) {
        log.info("用户登录请求: {}", user.getUsername());
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.fail("用户名或密码不能为空");
        }
        LoginResponse response = sysUserService.login(user.getUsername(), user.getPassword());
        if (response == null) {
            return Result.fail("用户名或密码错误");
        }
        return Result.success("登录成功", response);
    }

    @GetMapping("/info")
    public Result<SysUser> getUserInfo(HttpServletRequest request) {
        String authToken = request.getHeader("Authorization");
        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Result.fail("未提供有效的token");
        }
        String actualToken = authToken.substring(7);
        String username = jwtUtil.getUsernameFromToken(actualToken);
        if (username == null) {
            return Result.fail("无效的token");
        }
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success("获取成功", user);
    }

}
