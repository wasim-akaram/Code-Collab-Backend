package com.codesync.payment.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    /** Plan to purchase: "PRO_MONTHLY" */
    private String planId;
}
