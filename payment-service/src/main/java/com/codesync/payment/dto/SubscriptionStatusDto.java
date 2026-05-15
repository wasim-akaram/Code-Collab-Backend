package com.codesync.payment.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusDto {
    private String plan;
    private String status;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
}
