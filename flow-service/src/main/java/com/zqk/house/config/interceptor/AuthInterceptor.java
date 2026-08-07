package com.zqk.house.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.zqk.house.config.SuperAdminProperties;
import com.zqk.house.sysuser.entity.SysUser;
import com.zqk.house.sysuser.service.SysUserService;
import com.zqk.house.sysuser.vo.LoginUser;
import com.zqk.house.util.JwtUtil;
import com.zqk.house.util.SecurityUtils;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SuperAdminProperties superAdminProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        // 登录接口放行
        if (request.getRequestURI().contains("/user/login")) {
            return true;
        }

        // 取 token（header 优先，兼容 query 参数）
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            return false;
        }

        // 解析 username，查 sys_user，将登录用户信息存入 ThreadLocal
        String username = jwtUtil.getUsernameFromToken(token);
        if (StringUtils.hasText(username)) {
            SysUser sysUser = sysUserService.findByUsername(username);
            if (sysUser != null) {
                LoginUser loginUser = new LoginUser();
                loginUser.setId(sysUser.getId());
                loginUser.setUsername(sysUser.getUsername());
                loginUser.setEmpNo(sysUser.getEmpNo());
                loginUser.setRealName(sysUser.getRealName());
                loginUser.setDeptId(sysUser.getDeptId());
                loginUser.setDeptName(sysUser.getDeptName());
                loginUser.setSuperAdmin(superAdminProperties.isSuperAdmin(sysUser.getEmpNo(), sysUser.getRealName()));
                SecurityUtils.setLoginUser(loginUser);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，防止内存泄漏
        SecurityUtils.clear();
    }
}
