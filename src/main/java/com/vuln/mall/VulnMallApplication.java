package com.vuln.mall;

import com.vuln.mall.entity.Order;
import com.vuln.mall.entity.Product;
import com.vuln.mall.entity.User;
import com.vuln.mall.repository.OrderRepository;
import com.vuln.mall.repository.ProductRepository;
import com.vuln.mall.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Arrays;

@SpringBootApplication
public class VulnMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(VulnMallApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(UserRepository userRepository, ProductRepository productRepository,
            OrderRepository orderRepository) {
        return (args) -> {
            // 1. Create Users
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("admin123"); // Plain text vulnerability
                admin.setName("관리자");
                admin.setEmail("admin@securecorp.com");
                admin.setRole("ADMIN");
                admin.setAddress("서울특별시 강남구 테헤란로 123");
                userRepository.save(admin);

                User user = new User();
                user.setUsername("user");
                user.setPassword("user123");
                user.setName("홍길동");
                user.setEmail("hong@example.com");
                user.setRole("USER");
                user.setAddress("경기도 성남시 분당구 판교역로 456");
                userRepository.save(user);
            }

            // 2. Create Products
            if (productRepository.count() == 0) {
                Product p1 = new Product();
                p1.setName("게이밍 노트북 Pro X");
                p1.setDescription("최신 RTX 4090 탑재, 초고성능 게이밍 노트북");
                p1.setPrice(3500000);
                p1.setImageUrl("https://placehold.co/600x400?text=Gaming+Laptop");

                Product p2 = new Product();
                p2.setName("스마트워치 Ultra");
                p2.setDescription("티타늄 바디와 100시간 배터리 수명");
                p2.setPrice(890000);
                p2.setImageUrl("https://placehold.co/600x400?text=Smartwatch");

                Product p3 = new Product();
                p3.setName("무선 노이즈캔슬링 헤드폰");
                p3.setDescription("업계 최고의 노이즈 캔슬링 기술 적용");
                p3.setPrice(450000);
                p3.setImageUrl("https://placehold.co/600x400?text=Headphones");

                Product p4 = new Product();
                p4.setName("4K 전문가용 모니터");
                p4.setDescription("정확한 색재현율, 디자이너를 위한 최고의 선택");
                p4.setPrice(1200000);
                p4.setImageUrl("https://placehold.co/600x400?text=4K+Monitor");

                productRepository.saveAll(Arrays.asList(p1, p2, p3, p4));
            }

            // 3. Create Orders
            if (orderRepository.count() == 0) {
                User user = userRepository.findByUsername("user");
                if (user != null) {
                    Order o1 = new Order();
                    o1.setUserId(user.getId());
                    o1.setProductId(1L);
                    o1.setQuantity(1);
                    o1.setTotalPrice(3500000);
                    o1.setOrderDate(LocalDateTime.now().minusDays(2));
                    o1.setStatus("DELIVERED");
                    orderRepository.save(o1);

                    Order o2 = new Order();
                    o2.setUserId(user.getId());
                    o2.setProductId(3L);
                    o2.setQuantity(2);
                    o2.setTotalPrice(900000);
                    o2.setOrderDate(LocalDateTime.now().minusHours(5));
                    o2.setStatus("ORDERED");
                    orderRepository.save(o2);
                }
            }
        };
    }
}
