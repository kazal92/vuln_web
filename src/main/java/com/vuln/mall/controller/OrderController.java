package com.vuln.mall.controller;

import com.vuln.mall.entity.Order;
import com.vuln.mall.entity.Product;
import com.vuln.mall.repository.OrderRepository;
import com.vuln.mall.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Controller
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/order/create")
    public String createOrder(@RequestParam Long productId, @RequestParam int quantity,
            jakarta.servlet.http.HttpServletRequest request) { // Use Request to get Cookies

        Long userId = getUserIdFromCookie(request);
        if (userId == null) {
            return "redirect:/login";
        }

        Product product = productRepository.findById(productId).orElseThrow();

        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalPrice(product.getPrice() * quantity);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("ORDERED");

        orderRepository.save(order);

        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String listOrders(jakarta.servlet.http.HttpServletRequest request, Model model) {
        Long userId = getUserIdFromCookie(request);
        if (userId == null)
            return "redirect:/login";

        List<Order> orders = orderRepository.findByUserId(userId);
        model.addAttribute("orders", orders);
        return "order_list";
    }

    @GetMapping("/order/detail")
    public String orderDetail(@RequestParam Long id, Model model) {
        // 취약점: 부적절한 인가 (IDOR)
        // 1. URL 파라미터로 'id'를 입력받습니다.
        // 2. DB에서 주문 정보를 직접 조회합니다.
        // 3. 치명적: 조회된 주문이 현재 로그인한 사용자의 것인지 확인하지 않습니다.
        // 공격: ?id=1 을 ?id=2 로 변경하여 다른 사용자의 주문을 조회할 수 있습니다.

        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        model.addAttribute("order", order);
        return "order_detail";
    }

    @GetMapping("/order/cancel")
    public String cancelOrder(@RequestParam Long id) {
        // 취약점: IDOR + CSRF
        // 1. 'id' 파라미터를 입력받습니다.
        // 2. 소유권 확인 없음 (IDOR).
        // 3. GET 요청으로 상태를 변경함 (CSRF).
        // 공격: 공격자는 사용자에게 /order/cancel?id=100 링크를 클릭하게 유도하여
        // 타인의 주문을 취소시킬 수 있습니다.

        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        return "redirect:/orders";
    }

    private Long getUserIdFromCookie(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        return java.util.Arrays.stream(request.getCookies())
                .filter(c -> "user_id".equals(c.getName()))
                .findFirst()
                .map(jakarta.servlet.http.Cookie::getValue)
                .map(Long::parseLong)
                .orElse(null);
    }
}
