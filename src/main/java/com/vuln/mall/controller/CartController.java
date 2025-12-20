package com.vuln.mall.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Base64;

@Controller
public class CartController {

    // 취약점: 안전하지 않은 역직렬화 (Insecure Deserialization)
    // "상태 유지"를 위해 자바 직렬화 객체(Java Serialized Object)를 쿠키에 저장합니다.
    // 이를 다시 읽어들일 때 검증(whitelist) 없이 readObject()를 사용합니다.
    // 공격자는 악성 페이로드(CommonsCollections 등)를 제공하여 코드를 실행할 수 있습니다.

    @GetMapping("/cart/save")
    @ResponseBody
    public String saveCartState(HttpServletResponse response) throws IOException {
        // 직렬화할 더미 객체 생성
        CartState state = new CartState();
        state.username = "guest";
        state.itemCount = 5;

        // 직렬화 (Serialize)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(state);
        oos.close();

        // Base64 인코딩
        String token = Base64.getEncoder().encodeToString(baos.toByteArray());

        // Set Cookie
        Cookie cookie = new Cookie("cart_state", token);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        response.addCookie(cookie);

        return "장바구니 상태가 쿠키에 저장되었습니다! (자바 직렬화 객체)";
    }

    @GetMapping("/cart/restore")
    @ResponseBody
    public String restoreCartState(@CookieValue(value = "cart_state", required = false) String token) {
        if (token == null) {
            return "장바구니 상태를 찾을 수 없습니다.";
        }

        try {
            // Base64 디코딩
            byte[] data = Base64.getDecoder().decode(token);

            // 역직렬화 (취약점 발생 지점)
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject(); // 악성 페이로드일 경우 여기서 RCE 발생

            if (obj instanceof CartState) {
                CartState state = (CartState) obj;
                return "장바구니 복구됨: " + state.username + ", 아이템 수: " + state.itemCount;
            } else {
                return "알 수 없는 객체 타입입니다.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "역직렬화 실패: " + e.getMessage();
        }
    }

    // 기능적으로는 문제없는 클래스지만, 취약점은 프로세스 과정에 있습니다.
    // 이 클래스 자체는 무해하더라도, 공격은 클래스패스 상의 다른 클래스(가젯)를 통해 이루어집니다.
    static class CartState implements Serializable {
        private static final long serialVersionUID = 1L;
        public String username;
        public int itemCount;
    }
}
