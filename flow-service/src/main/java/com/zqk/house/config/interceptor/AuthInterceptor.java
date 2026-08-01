package com.zqk.house.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.zqk.house.util.JwtUtil;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println("[AuthInterceptor] " + request.getMethod() + " " + request.getRequestURI());
        // Handle OPTIONS request
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // Handle login request
        if (request.getRequestURI().contains("/user/login")) {
            return true;
        }
        
        // Get token from header or parameter
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }
        
        if (!StringUtils.hasText(token)) {
            response.setStatus(401);
            return false;
        }
        
        if (!token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        
        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            return false;
        }
        
        return true;
    }
} 