package com.codesync.payment.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    /** Plan name: PRO */
    @Column(length = 20, nullable = false)
    private String plan;

    /** ACTIVE, EXPIRED, CANCELLED */
    @Column(length = 20, nullable = false)
    private String status;

    /** Amount in paise (e.g. 49900 = ₹499) */
    private Integer amountPaise;

    private Instant startDate;
    private Instant endDate;

    /** Razorpay order ID that initiated this subscription */
    private String razorpayOrderId;

    /** Razorpay payment ID after successful payment */
    private String razorpayPaymentId;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); this.updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
