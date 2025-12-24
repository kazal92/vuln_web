package com.vuln.mall.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ToolController {

    @GetMapping("/tools/preview")
    public String previewPage(Model model) {
        // Subtle: No pre-filled template that screams "SSTI"
        model.addAttribute("template", "가입을 환영합니다! 저희 쇼핑몰을 이용해주셔서 감사합니다.");
        return "tool_preview";
    }

    @PostMapping("/tools/preview")
    public String previewTemplate(@RequestParam String template, Model model) {
        // 취약점: 서버 사이드 템플릿 인젝션 (SSTI) - SpEL 사용
        // 과거에 "유연한 알림 템플릿"을 위해 구현되었다는 설정

        String result;
        try {
            org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();

            // 배경 컨텍스트 데이터 (UI에는 숨겨져 있음)
            java.util.Map<String, Object> mallData = new java.util.HashMap<>();
            mallData.put("name", "시큐어코프 몰");
            mallData.put("url", "https://securecorp-mall.com");
            mallData.put("contact", "02-1234-5678");

            java.util.Map<String, Object> userData = new java.util.HashMap<>();
            userData.put("name", "홍길동");
            userData.put("email", "hong@example.com");

            java.util.Map<String, Object> root = new java.util.HashMap<>();
            root.put("mall", mallData);
            root.put("user", userData);
            root.put("date", java.time.LocalDate.now().toString());

            org.springframework.expression.spel.support.StandardEvaluationContext context = new org.springframework.expression.spel.support.StandardEvaluationContext(
                    root);
            context.addPropertyAccessor(new org.springframework.context.expression.MapAccessor());

            // 표준 TemplateParserContext를 사용하여 #{...} 내부를 SpEL로 평가
            // 가장 기본적이고 교과서적인 형태의 취약Thymeleaf점 코드입니다.
            result = parser.parseExpression(template, new org.springframework.expression.common.TemplateParserContext())
                    .getValue(context, String.class);
        } catch (Exception e) {
            // 디버깅을 위해 상세 에러 메시지 출력
            result = "오류 발생: " + e.getMessage();
            if (e.getCause() != null) {
                result += " (원인: " + e.getCause().getMessage() + ")";
            }
            e.printStackTrace(); // 서버 로그에도 출력
        }

        model.addAttribute("template", template);
        model.addAttribute("result", result);
        return "tool_preview";
    }
}
