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

            // ${...} 마커가 있으면 처리
            if (template.contains("${")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^\\}]+)\\}");
                java.util.regex.Matcher matcher = pattern.matcher(template);
                java.lang.StringBuffer sb = new java.lang.StringBuffer();
                while (matcher.find()) {
                    String expression = matcher.group(1).trim();
                    try {
                        Object value = parser.parseExpression(expression).getValue(context);
                        matcher.appendReplacement(sb,
                                value != null ? java.util.regex.Matcher.quoteReplacement(value.toString()) : "");
                    } catch (Exception e) {
                        matcher.appendReplacement(sb, "[파싱 오류]");
                    }
                }
                matcher.appendTail(sb);
                result = sb.toString();
            } else {
                result = template; // 템플릿 마커가 없으면 그대로 반환
            }
        } catch (Exception e) {
            result = "서버 내부 오류가 발생했습니다.";
        }

        model.addAttribute("template", template);
        model.addAttribute("result", result);
        return "tool_preview";
    }
}
