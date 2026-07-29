package com.zqk.house.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.zqk.house.user.entity.User;
import com.zqk.house.user.entity.UserForm;
import com.zqk.house.user.service.UserService;
import com.zqk.house.user.vo.LoginResponse;
import com.zqk.house.util.JwtUtil;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.Result;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {
    
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
        
    // 基础CRUD操作
    @PostMapping("/add")
    public Result<Void> addUser(@RequestBody User user) {
        return userService.addUser(user) ? 
            Result.success("添加成功") : 
            Result.fail("添加失败");
    }
        
    @PutMapping("/update")
    public Result<Void> updateUser(@RequestBody User user) {
        // 参数校验
        if (user.getId() == null) {
            return Result.fail("ID不能为空");
        }
        
        // 检查用户是否存在
        User existingUser = userService.getUserById(user.getId());
        if (existingUser == null) {
            return Result.notFound("用户不存在");
        }
        
        return userService.updateUser(user) ? 
            Result.success("更新成功") : 
            Result.fail("更新失败");
    }
        
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id) ? 
            Result.success("删除成功") : 
            Result.fail("删除失败");
    }
        
    @GetMapping("/{docNumber}")
    public Result<User> getUserByDocNumber(@PathVariable String docNumber) {
        User user = userService.getUserByDocNumber(docNumber);
        return user != null ? 
            Result.success("获取成功", user) : 
            Result.notFound("用户不存在");
    }
        
    // 查询相关
    @PostMapping("/list")
    public Result<PageResult<User>> getUserList(@RequestBody UserForm userForm) {
        PageResult<User> pageResult = userService.getUserList(userForm);
        return Result.success("获取成功", pageResult);
    }
        
    // 认证相关
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody User user) {
        log.info("用户登录请求");
        
        // 参数校验
        if (!StringUtils.hasText(user.getUserName()) || !StringUtils.hasText(user.getPassword())) {
            return Result.fail("用户名或密码不能为空");
        }
        
        // 查询用户
        User dbUser = userService.getUserByUsername(user.getUserName());
        if (dbUser == null) {
            return Result.fail("用户不存在");
        }
        
        // 密码校验
        if (!dbUser.getPassword().equals(user.getPassword())) {
            return Result.fail("密码错误");
        }
        
        // 生成JWT token
        String token = jwtUtil.generateToken(user.getUserName());
        
        // 清空密码
        dbUser.setPassword(null);
        
        // 返回token和用户信息
        LoginResponse response = new LoginResponse("Bearer " + token, dbUser);
        return Result.success("登录成功", response);
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        log.info("Authorization header: {}", request.getHeader("Authorization"));
        
        // 优先从请求头获取token，如果没有则从URL参数获取
        String authToken = request.getHeader("Authorization");
        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Result.fail("未提供有效的token: " + authToken);
        }
        
        // 去掉"Bearer "前缀
        String actualToken = authToken.substring(7);
        log.info("Parsed token: {}", actualToken);
        
        String userName = jwtUtil.getUsernameFromToken(actualToken);
        log.info("Username from token: {}", userName);
        
        if (userName == null) {
            return Result.fail("无效的token");
        }
        
        User user = userService.getUserByUsername(userName);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        
        user.setPassword(null);
        return Result.success("获取成功", user);
    }

}
