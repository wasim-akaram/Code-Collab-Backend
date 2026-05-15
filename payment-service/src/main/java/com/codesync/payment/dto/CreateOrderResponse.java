package com.codesync.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    private String orderId;
    private int amountPaise;
    private String currency;
    private String razorpayKeyId;
    private String planName;
    private String description;
}
