package com.vuln.mall.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * [Partner API] 외부 파트너사가 쇼핑몰 상품 데이터를 검색할 수 있도록 제공하는 API
 * Legacy 시스템 연동을 위해 JSONP 방식을 지원 (취약점 원인)
 */
@Controller
public class RFDController {

    // 개발자 콘솔 페이지
    @GetMapping("/vuln/rfd")
    public String apiConsole() {
        return "rfd";
    }

    /**
     * Partner Product Search API (v1)
     * Endpoint: /api/v1/partner/products/search
     * 
     * @param query    검색어 (예: laptop)
     * @param callback JSONP 콜백 함수명 (Legacy Support)
     */
    @GetMapping(value = { "/api/v1/partner/products/search", "/api/v1/partner/products/search/**" })
    @ResponseBody
    public String searchProducts(@RequestParam(value = "q", required = false, defaultValue = "") String query,
            @RequestParam(value = "callback", required = false) String callback,
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. 실제 상품 데이터 검색 로직 (Mock Data)
        List<Map<String, Object>> products = new ArrayList<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", 101);
        p1.put("name", "Premium " + (query.isEmpty() ? "Notebook" : query));
        p1.put("price", 1200000);
        p1.put("stock", 50);
        products.add(p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("id", 102);
        p2.put("name", "Gaming Mouse");
        p2.put("price", 89000);
        p2.put("stock", 200);
        products.add(p2);

        // 결과 JSON 생성
        String jsonResult = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", "success");
            responseMap.put("data", products);
            jsonResult = mapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            jsonResult = "{\"error\": \"Internal Server Error\"}";
        }

        // 2. RFD 취약점 시뮬레이션 로직
        // 브라우저가 URL 확장자(.bat, .cmd)를 보고 실행 파일로 오인하게 만듬
        String uri = request.getRequestURI();
        if (uri.endsWith(".bat") || uri.endsWith(".cmd")) {
            // 서버 설정 미흡 시뮬레이션: 실행 파일 확장자에 대해 Content-Disposition을 attachment로 강제
            String filename = uri.substring(uri.lastIndexOf("/") + 1);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentType("application/octet-stream");
        } else {
            // 정상적인 API 호출 시 JSON Content-Type 설정
            response.setContentType("application/json; charset=UTF-8");
        }

        // 3. JSONP 지원 (Reflected Value)
        // 사용자가 전달한 callback 파라미터를 검증 없이 응답 앞부분에 붙임
        if (callback != null && !callback.isEmpty()) {
            // 예: callback=myFunction -> myFunction({...json...});
            // 취약점: callback=calc|| -> calc||({...json...}); (배치 파일 문법으로 해석되어 실행됨)
            return callback + "(" + jsonResult + ");";
        }

        return jsonResult;
    }
}
