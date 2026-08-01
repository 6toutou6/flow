package com.zqk.house.util;

import com.zqk.house.sysuser.vo.LoginUser;

/**
 * 登录用户上下文工具类（基于 ThreadLocal）
 * AuthInterceptor 在 preHandle 解析 token 后填充，afterCompletion 清理
 * 业务方法调用 SecurityUtils.getLoginUser() 即可获取当前登录用户
 */
public class SecurityUtils {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void setLoginUser(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser getLoginUser() {
        return HOLDER.get();
    }

    public static String getCurrentUsername() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUsername();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
