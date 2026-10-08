package com.charity.reunion.security;

import lombok.Data;

/**
 * 当前登录用户上下文(请求级 ThreadLocal)
 */
public class UserContext {

    @Data
    public static class LoginUser {
        private Long id;
        private String role;
    }

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 当前登录用户ID,未登录抛异常 */
    public static Long requireId() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new com.charity.reunion.common.BizException("请先登录");
        }
        return u.getId();
    }

    /** 当前登录用户ID,未登录返回 null(供可选登录场景) */
    public static Long getId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
