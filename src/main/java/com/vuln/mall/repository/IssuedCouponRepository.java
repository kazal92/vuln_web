package com.vuln.mall.repository;

import com.vuln.mall.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
    boolean existsByCouponIdAndUsername(Long couponId, String username);

    List<IssuedCoupon> findByCouponId(Long couponId);

    long countByCouponIdAndUsername(Long couponId, String username);
}
