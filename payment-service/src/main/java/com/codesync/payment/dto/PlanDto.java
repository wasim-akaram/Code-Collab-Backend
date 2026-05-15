package com.codesync.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDto {
    private String planId;
    private String name;
    private String description;
    private int amountPaise;
    private String currency;
    private int durationDays;
    private java.util.List<String> features;
}
