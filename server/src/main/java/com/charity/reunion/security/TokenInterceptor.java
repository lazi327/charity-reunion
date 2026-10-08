package com.charity.reunion.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录令牌拦截器:
 * - 校验 Authorization: Bearer <token>
 * - /api/admin/** 额外要求 ADMIN 角色
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public TokenInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parse(auth.substring(7));
            if (claims != null) {
                UserContext.LoginUser user = new UserContext.LoginUser();
                user.setId(Long.valueOf(claims.getSubject()));
                user.setRole(claims.get("role", String.class));
                UserContext.set(user);

                String uri = req.getRequestURI();
                if (uri.startsWith("/api/admin") && !"ADMIN".equals(user.getRole())) {
                    writeJson(resp, 403, "无权访问管理端");
                    return false;
                }
                return true;
            }
        }
        writeJson(resp, 401, "未登录或登录已过期");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeJson(HttpServletResponse resp, int code, String msg) throws Exception {
        resp.setStatus(200);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
    }
}
