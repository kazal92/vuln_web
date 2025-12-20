package com.vuln.mall.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Just storing ID for simplicity/vulnerability (no foreign key constraint
                         // enforcement)
    private Long productId;
    private int quantity;
    private int totalPrice;
    private LocalDateTime orderDate;
    private String status; // "ORDERED", "CANCELLED"
}
