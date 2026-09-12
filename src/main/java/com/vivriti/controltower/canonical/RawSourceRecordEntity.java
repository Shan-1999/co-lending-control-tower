package com.vivriti.controltower.canonical;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "raw_source_record", uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "payload_hash"}))
public class RawSourceRecordEntity {

    @Id
    @Column(name = "record_id", length = 64)
    private String recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private SourceBatchEntity batch;

    @Column(name = "source_system", length = 32)
    private String sourceSystem;

    @Column(name = "payload_hash", length = 64, nullable = false)
    private String payloadHash;

    @Column(name = "raw_payload", columnDefinition = "TEXT", nullable = false)
    private String rawPayload;

    @Column(name = "quarantined")
    private boolean quarantined = false;

    @Column(name = "quarantine_reason", columnDefinition = "TEXT")
    private String quarantineReason;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public SourceBatchEntity getBatch() { return batch; }
    public void setBatch(SourceBatchEntity batch) { this.batch = batch; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public boolean isQuarantined() { return quarantined; }
    public void setQuarantined(boolean quarantined) { this.quarantined = quarantined; }

    public String getQuarantineReason() { return quarantineReason; }
    public void setQuarantineReason(String quarantineReason) { this.quarantineReason = quarantineReason; }

    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getBatchId() {
        return batch != null ? batch.getBatchId() : null;
    }
}
