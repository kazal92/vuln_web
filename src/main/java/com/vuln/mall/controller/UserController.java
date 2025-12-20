package com.vuln.mall.controller;

import com.vuln.mall.entity.User;
import com.vuln.mall.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
            HttpServletResponse response, HttpSession session, Model model) {

        // 취약점: SQL 인젝션 (SQL Injection)
        // PreparedStatement 대신 Statement와 문자열 결합을 사용하고 있습니다.
        String sql = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                // Login Success
                Long userId = rs.getLong("id");
                String role = rs.getString("role");
                String name = rs.getString("name");

                // 취약점: 깨진 인증 / 안전하지 않은 쿠키 (Broken Authentication / Insecure Cookies)
                // 암호화나 서명 없이 민감한 데이터를 쿠키에 설정함
                Cookie userCookie = new Cookie("user_id", String.valueOf(userId));
                userCookie.setPath("/");
                response.addCookie(userCookie);

                Cookie roleCookie = new Cookie("role", role);
                roleCookie.setPath("/");
                response.addCookie(roleCookie);

                // Also set in session for convenience in Thymeleaf, but cookies are the vector
                session.setAttribute("user", name);
                session.setAttribute("role", role);

                return "redirect:/";
            } else {
                model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
                return "login";
            }

        } catch (Exception e) {
            // 취약점: 정보 노출 (Information Leakage)
            // 스택 트레이스나 SQL 에러를 사용자에게 그대로 노출함
            model.addAttribute("error", "데이터베이스 오류: " + e.getMessage());
            return "login";
        }
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute User user) {
        // 취약점: 대량 할당 (Mass Assignment)
        // 요청 파라미터로 user 객체를 직접 바인딩합니다.
        // 공격자가 "role=ADMIN"을 보내면 그대로 저장됩니다.

        if (user.getRole() == null) {
            user.setRole("USER");
        }

        userRepository.save(user);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response, HttpSession session) {
        Cookie cookie = new Cookie("user_id", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        Cookie roleCookie = new Cookie("role", null);
        roleCookie.setMaxAge(0);
        roleCookie.setPath("/");
        response.addCookie(roleCookie);

        session.invalidate();
        return "redirect:/";
    }
}
