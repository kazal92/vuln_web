package com.vuln.mall.controller;

import com.vuln.mall.entity.Product;
import com.vuln.mall.entity.Review;
import com.vuln.mall.repository.ProductRepository;
import com.vuln.mall.repository.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/review/create")
    public String createReview(@RequestParam Long productId,
            @RequestParam String content,
            HttpSession session) {

        String username = (String) session.getAttribute("user");
        if (username == null) {
            return "redirect:/login";
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Review review = new Review();
            review.setProduct(product);
            review.setUsername(username);

            // 취약점: 저장형 XSS (Stored XSS)
            // 'content'에 대해 아무런 살균(sanitization) 처리를 하지 않습니다.
            // 스크립트 태그(예: <script>alert(1)</script>)가 그대로 저장됩니다.
            review.setContent(content);

            reviewRepository.save(review);
        }

        return "redirect:/product/detail?id=" + productId;
    }

    @PostMapping("/review/delete")
    public String deleteReview(@RequestParam Long id, @RequestParam Long productId) {
        // 취약점: 부적절한 인가 (IDOR)
        // 소유권 확인 없이 ID만으로 리뷰를 삭제합니다.
        // 공격자는 이 엔드포인트에 POST 요청을 보내 아무 리뷰나 삭제할 수 있습니다.
        reviewRepository.deleteById(id);

        return "redirect:/product/detail?id=" + productId;
    }

    @PostMapping("/review/update")
    public String updateReview(@RequestParam Long id,
            @RequestParam Long productId,
            @RequestParam String content) {
        // 취약점: IDOR + 저장형 XSS
        // 소유권 확인 없이 ID로 리뷰를 수정합니다.
        Review review = reviewRepository.findById(id).orElse(null);
        if (review != null) {
            review.setContent(content); // Stored XSS: content is not sanitized
            reviewRepository.save(review);
        }

        return "redirect:/product/detail?id=" + productId;
    }
}
