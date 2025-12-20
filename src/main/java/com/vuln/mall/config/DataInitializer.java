package com.vuln.mall.config;

import com.vuln.mall.entity.Product;
import com.vuln.mall.entity.User;
import com.vuln.mall.repository.ProductRepository;
import com.vuln.mall.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

        @Bean
        public CommandLineRunner initData(UserRepository userRepository, ProductRepository productRepository) {
                return args -> {
                        // Check if data already exists to avoid duplicates/errors
                        if (userRepository.count() > 0) {
                                return;
                        }

                        // Create Admin User
                        User admin = new User();
                        admin.setUsername("admin");
                        admin.setPassword("admin123"); // Plain text
                        admin.setName("관리자");
                        admin.setEmail("admin@securecorp.com");
                        admin.setRole("ADMIN");
                        admin.setAddress("관리자 본부, 비밀 장소");
                        userRepository.save(admin);

                        // Create Normal User
                        User user = new User();
                        user.setUsername("guest");
                        user.setPassword("guest123");
                        user.setName("게스트 사용자");
                        user.setEmail("guest@securecorp.com");
                        user.setRole("USER");
                        user.setAddress("서울시 강남구 테헤란로 123");
                        userRepository.save(user);

                        // Create Products
                        createProduct(productRepository, "프리미엄 노트북",
                                        "게임 및 업무용 고성능 노트북입니다.", 1500000,
                                        "/images/laptop.png");
                        createProduct(productRepository, "스마트폰 X", "AI 카메라가 탑재된 최신 스마트폰입니다.", 1000000,
                                        "/images/smartphone.png");
                        createProduct(productRepository, "무선 헤드폰", "노이즈 캔슬링 기능이 있는 무선 헤드폰입니다.", 200000,
                                        "/images/headphones.png");
                        createProduct(productRepository, "게이밍 마우스", "높은 DPI와 RGB 조명이 있는 게이밍 마우스입니다.", 50000,
                                        "/images/mouse.png");
                        createProduct(productRepository, "기계식 키보드", "타건감이 좋은 기계식 키보드입니다.", 120000,
                                        "/images/keyboard.png");
                };
        }

        private void createProduct(ProductRepository repo, String name, String desc, int price, String img) {
                Product p = new Product();
                p.setName(name);
                p.setDescription(desc);
                p.setPrice(price);
                p.setImageUrl(img);
                repo.save(p);
        }
}
