package com.codesync.payment.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    /** Amount in paise */
    private Integer amountPaise;

    /** INR */
    @Column(length = 10)
    private String currency;

    /** CREATED, SUCCESS, FAILED */
    @Column(length = 20, nullable = false)
    private String status;

    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
