package com.codesync.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codesync.payment.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByUserEmailAndStatusOrderByEndDateDesc(String userEmail, String status);
    List<Subscription> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
