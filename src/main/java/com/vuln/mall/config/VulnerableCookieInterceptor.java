package com.vuln.mall.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VulnerableCookieInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            HttpSession session = request.getSession();
            for (Cookie cookie : cookies) {
                // VULNERABILITY: Blindly trusting cookies to set session state
                if ("role".equals(cookie.getName())) {
                    session.setAttribute("role", cookie.getValue());
                }
                if ("user_id".equals(cookie.getName())) {
                    session.setAttribute("user_id", cookie.getValue());
                }
                if ("user".equals(cookie.getName())) {
                    // Optionally trust the username cookie too if it exists
                    // session.setAttribute("user", cookie.getValue());
                }
            }
        }
        return true;
    }
}
