package com.company.flow.sys.base.config;

import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.context.UserContext;
import com.company.flow.sys.base.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录拦截器：登录成功后用户写入会话（SESSION_USER_KEY），
 * 本拦截器在每个请求开始时取出并写入 UserContext / SecurityUtils，未登录返回 401。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 会话中存放登录用户的 key */
    public static final String SESSION_USER_KEY = "loginUser";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AdminProperties adminProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", 401);
            body.put("message", "未登录或登录已过期");
            body.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }
        // 写入上下文
        UserContext.set(user);
        LoginUser loginUser = new LoginUser();
        loginUser.setYyytId(user.getYyytId());
        loginUser.setUserName(user.getUserName());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setDeptName(user.getDeptName());
        loginUser.setSuperAdmin(adminProperties.isSuperAdmin(user.getYyytId()));
        SecurityUtils.setLoginUser(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
        SecurityUtils.clear();
    }
}
