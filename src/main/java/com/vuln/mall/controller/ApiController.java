package com.vuln.mall.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.vuln.mall.repository.UserRepository;
import com.vuln.mall.entity.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    @Autowired
    private UserRepository userRepository;

    // 취약점: 기능 수준의 접근 제어 미흡 (Broken Function Level Access Control)
    // 이 엔드포인트는 "숨겨져" 있으며(UI에 없음) ADMIN 확인이 없습니다.
    // 공격자는 이를 발견(JS 파일 분석 또는 추측)하여 자신을 관리자로 승격시킬 수 있습니다.
    @PostMapping("/user/promote")
    public String promoteUser(@RequestParam String username) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            user.setRole("ADMIN");
            userRepository.save(user);
            return "성공: 사용자 " + username + " 님이 관리자(ADMIN)가 되었습니다.";
        }
        return "오류: 사용자를 찾을 수 없습니다.";
    }

    // --- JWT Section ---

    // 비밀 키 (약점: 매우 단순하여 무차별 대입하기 쉬움)
    private static final String SECRET_KEY = "secret123";

    @PostMapping("/auth/token")
    public Map<String, String> generateToken(@RequestParam String username) {
        // 취약점: 약한 비밀 키 & 검증 부재
        // 모바일 앱 테스트용 토큰 생성

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return response;
    }

    @GetMapping("/mobile/profile")
    public Map<String, Object> getMobileProfile(@RequestHeader("Authorization") String token) {
        // 취약점: JWT 약점 (None 알고리즘 혹은 약한 키)
        // 여기서는 약한 키('secret123') 취약점을 보여줍니다.
        // 또한, 설정을 통해 None 알고리즘을 허용할 수도 있습니다.
        // (jjwt는 기본적으로 none을 막지만, 로직 우회 상황을 가정)

        Map<String, Object> result = new HashMap<>();
        try {
            // 토큰 검증
            // 취약한 시나리오: 서명 검증 없이 디코딩하거나, 'none'을 허용하는 파서를 사용
            // 여기서는 약한 키를 사용합니다. 공격자가 'secret123'을 알아내면 토큰을 위조할 수 있습니다.

            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(token.replace("Bearer ", ""));

            String username = claims.getBody().getSubject();
            result.put("status", "success");
            result.put("username", username);
            result.put("message", "모바일 프로필 데이터 접근 성공.");

        } catch (JwtException e) {
            result.put("status", "error");
            result.put("message", "유효하지 않은 토큰: " + e.getMessage());
        }

        return result;
    }
}
