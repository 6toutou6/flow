package com.company.flow.sys.base.util;

import com.company.flow.sys.base.autuser.vo.LoginUser;

/**
 * 当前登录用户访问器（基于 ThreadLocal）
 * AuthInterceptor 在 preHandle 从会话取出登录用户后填充，afterCompletion 清理
 */
public class SecurityUtils {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void setLoginUser(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser getLoginUser() {
        return HOLDER.get();
    }

    /** 当前登录用户号（yyyt_id；未登录返回 null） */
    public static String getCurrentYyytId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getYyytId();
    }

    /** 当前登录用户姓名（yyyt_id；未登录返回 null） */
    public static String getCurrentUserName() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserName();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
