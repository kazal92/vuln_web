package com.vuln.mall.controller;

import com.vuln.mall.entity.Product;
import com.vuln.mall.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private javax.sql.DataSource dataSource;

    @GetMapping("/")
    public String home(Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam String q, Model model) {
        // 취약점: 반사형 XSS (Reflected XSS)
        // 검색어 'q'가 뷰에 직접 전달되어 th:utext로 렌더링됩니다.
        model.addAttribute("query", q);

        List<Product> products = new java.util.ArrayList<>();

        // 취약점: SQL 인젝션 (SQL Injection)
        // 원시 JDBC와 문자열 결합을 사용하여 악용을 허용합니다.
        // 공격자는 UNION SELECT를 사용하여 다른 테이블(예: users)의 데이터를 추출할 수 있습니다.
        String sql = "SELECT * FROM products WHERE name LIKE '%" + q + "%'";

        try (java.sql.Connection conn = dataSource.getConnection();
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getLong("id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setPrice(rs.getInt("price"));
                // 참고: 기본 Hibernate 네이밍 전략에 따라 컬럼명이 'image_url'일 가능성이 높음
                try {
                    p.setImageUrl(rs.getString("image_url"));
                } catch (java.sql.SQLException e) {
                    // 컬럼명이 다를 경우(예: 'imageUrl')를 대비한 폴백
                    p.setImageUrl(rs.getString("imageUrl"));
                }
                products.add(p);
            }

        } catch (Exception e) {
            // 공격을 돕기 위해 에러를 표시 (Error-based SQLi 가능성)
            model.addAttribute("error", "데이터베이스 오류: " + e.getMessage());
        }

        model.addAttribute("products", products);

        return "search_result";
    }
}
