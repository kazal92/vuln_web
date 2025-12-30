package com.vuln.mall.controller;

import com.vuln.mall.entity.Coupon;
import com.vuln.mall.entity.IssuedCoupon;
import com.vuln.mall.repository.CouponRepository;
import com.vuln.mall.repository.IssuedCouponRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CouponController {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    private static final long EVENT_COUPON_ID = 1L;

    @PostConstruct
    public void init() {
        // 앱 시작 시 이벤트 쿠폰 생성 (없으면)
        if (!couponRepository.existsById(EVENT_COUPON_ID)) {
            Coupon coupon = new Coupon();
            coupon.setId(EVENT_COUPON_ID);
            coupon.setName("선착순 10명 한정 50% 할인 쿠폰"); // 100 -> 10 Title Update
            coupon.setRemainingCount(10); // 100 -> 10
            couponRepository.save(coupon);
        }
    }

    @GetMapping("/event/coupon")
    public String couponPage(
            @org.springframework.web.bind.annotation.CookieValue(value = "username", required = false) String username,
            Model model) {
        Coupon coupon = couponRepository.findById(EVENT_COUPON_ID).orElse(null);
        model.addAttribute("coupon", coupon);

        String currentUsername = (username != null && !username.isEmpty()) ? username : "";
        model.addAttribute("currentUsername", currentUsername);

        long myCount = issuedCouponRepository.countByCouponIdAndUsername(EVENT_COUPON_ID, currentUsername);
        model.addAttribute("myCouponCount", myCount);

        return "coupon";
    }

    /**
     * [VULNERABILITY] Race Condition + IDOR
     * 1. IDOR: username 파라미터를 검증 없이 사용 (타인 명의 발급 가능)
     * 2. Race Condition: '발급 확인'과 '발급 처리' 사이에 락(Lock)이 없음 -> 따닥(동시 요청) 시 중복 발급 가능
     */
    @PostMapping("/api/coupon/issue")
    @ResponseBody
    public String issueCoupon(@RequestParam String username) {
        try {
            // 1. [IDOR Point] 로그인된 사용자와 username이 일치하는지 확인하지 않음.
            if (username == null || username.isEmpty()) {
                return "로그인이 필요합니다.";
            }

            // 2. 쿠폰 정보 조회
            Coupon coupon = couponRepository.findById(EVENT_COUPON_ID).orElse(null);
            if (coupon == null) {
                return "쿠폰 이벤트가 종료되었습니다.";
            }

            // 3. [Race Condition Point 1] 이미 발급받았는지 체크
            // 이 체크와 아래의 저장이 동시에 일어나면 중복 발급됨
            // 1인 1매 제한 엄격 체크 시도 (그러나 Race Condition으로 뚫림)
            if (issuedCouponRepository.existsByCouponIdAndUsername(EVENT_COUPON_ID, username)) {
                return "이미 쿠폰을 발급받으셨습니다. (1인 1매 제한)";
            }

            // 4. [Race Condition Point 2] 재고 체크
            // 재고가 1개 남았을 때 10명이 동시에 들어오면 10명 다 통과함
            if (coupon.getRemainingCount() <= 0) {
                return "선착순 마감되었습니다.";
            }

            // --- Race Condition Window ---
            // 의도적으로 약간의 지연을 주어 취약점을 더 잘 터지게 함 (선택 사항)
            // Thread.sleep(10);

            // 5. 쿠폰 발급 (Insert)
            IssuedCoupon issued = new IssuedCoupon();
            issued.setCouponId(EVENT_COUPON_ID);
            issued.setUsername(username);
            issuedCouponRepository.save(issued);

            // 6. 재고 감소 (Update)
            coupon.setRemainingCount(coupon.getRemainingCount() - 1);
            couponRepository.save(coupon);

            return "축하합니다! 쿠폰이 발급되었습니다.";

        } catch (Exception e) {
            return "오류가 발생했습니다: " + e.getMessage();
        }
    }

    // 리셋 기능 (테스트 편의용)
    @GetMapping("/admin/coupon/reset")
    @ResponseBody
    public String resetCoupon() {
        issuedCouponRepository.deleteAll();

        Coupon coupon = couponRepository.findById(EVENT_COUPON_ID).orElse(new Coupon());
        coupon.setId(EVENT_COUPON_ID);
        coupon.setName("선착순 10명 한정 50% 할인 쿠폰");
        coupon.setRemainingCount(10);
        couponRepository.save(coupon);

        return "쿠폰 이벤트가 리셋되었습니다. (잔여: 10개, 발급 내역 삭제됨)";
    }
}
