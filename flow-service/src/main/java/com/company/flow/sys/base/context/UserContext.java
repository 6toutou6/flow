package com.company.flow.sys.base.context;

import com.company.flow.sys.base.autuser.entity.User;

/**
 * 当前登录用户上下文：由 AuthInterceptor 在请求开始时从会话写入，请求结束后清理。
 * 仅允许在 controller 层（含拦截器）读取；service 层禁止直接使用本类，
 * 所需登录用户信息一律由 controller 取出后通过方法参数传入。
 */
public class UserContext {

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    /** 当前登录用户号（yyyt_id，即 aut_user 主键；未登录时返回 null） */
    public static String getUserId() {
        User user = HOLDER.get();
        return user == null ? null : user.getYyytId();
    }

    /** 当前登录用户部门id（未登录时返回 null） */
    public static Long getDeptId() {
        User user = HOLDER.get();
        return user == null ? null : user.getDeptId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
