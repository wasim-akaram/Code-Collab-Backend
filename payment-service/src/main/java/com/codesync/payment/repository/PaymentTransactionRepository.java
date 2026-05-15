package com.codesync.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codesync.payment.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);
}
