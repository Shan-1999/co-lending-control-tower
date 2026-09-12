package com.vivriti.controltower.canonical;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "source_batch")
public class SourceBatchEntity {

    @Id
    @Column(name = "batch_id", length = 64)
    private String batchId;

    @Column(name = "source_system", length = 32, nullable = false)
    private String sourceSystem;

    @Column(name = "partner_code", length = 32, nullable = false)
    private String partnerCode;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "declared_count", nullable = false)
    private int declaredCount;

    @Column(name = "declared_amount_paise", nullable = false)
    private long declaredAmountPaise;

    @Column(name = "status", length = 32, nullable = false)
    private String status = "RECEIVED";

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

    public int getDeclaredCount() { return declaredCount; }
    public void setDeclaredCount(int declaredCount) { this.declaredCount = declaredCount; }

    public long getDeclaredAmountPaise() { return declaredAmountPaise; }
    public void setDeclaredAmountPaise(long declaredAmountPaise) { this.declaredAmountPaise = declaredAmountPaise; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }
}
