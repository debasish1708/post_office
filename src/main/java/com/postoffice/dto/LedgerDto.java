package com.postoffice.dto;

import com.postoffice.model.WalletLedger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerDto {
    private Long id;
    private BigDecimal amount;
    private String type;
    private String description;
    private LocalDateTime createdAt;

    public static LedgerDto from(WalletLedger row) {
        LedgerDto dto = new LedgerDto();
        dto.id = row.getId();
        dto.amount = row.getAmount();
        dto.type = row.getType();
        dto.description = row.getDescription();
        dto.createdAt = row.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
