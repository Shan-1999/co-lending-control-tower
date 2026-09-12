package com.vivriti.controltower.canonical;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "canonical_event")
public class CanonicalEventEntity {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_record_id")
    private RawSourceRecordEntity rawRecord;

    @Column(name = "business_event_id", length = 64)
    private String businessEventId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "loan_reference", length = 64)
    private String loanReference;

    @Column(name = "partner_code", length = 32)
    private String partnerCode;

    @Column(name = "source_system", length = 32)
    private String sourceSystem;

    @Column(name = "event_type", length = 32)
    private String eventType;

    @Column(name = "amount_paise")
    private long amountPaise;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "source_status", length = 32)
    private String sourceStatus;

    @Column(name = "canonical_status", length = 32)
    private String canonicalStatus;

    @Column(name = "reversal_reference", length = 64)
    private String reversalReference;

    @Column(name = "source_timestamp")
    private OffsetDateTime sourceTimestamp;

    @Column(name = "received_timestamp")
    private OffsetDateTime receivedTimestamp;

    @Column(name = "cutoff_timestamp")
    private OffsetDateTime cutoffTimestamp;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public RawSourceRecordEntity getRawRecord() { return rawRecord; }
    public void setRawRecord(RawSourceRecordEntity rawRecord) { this.rawRecord = rawRecord; }

    public String getBusinessEventId() { return businessEventId; }
    public void setBusinessEventId(String businessEventId) { this.businessEventId = businessEventId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getLoanReference() { return loanReference; }
    public void setLoanReference(String loanReference) { this.loanReference = loanReference; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(long amountPaise) { this.amountPaise = amountPaise; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }

    public String getCanonicalStatus() { return canonicalStatus; }
    public void setCanonicalStatus(String canonicalStatus) { this.canonicalStatus = canonicalStatus; }

    public String getReversalReference() { return reversalReference; }
    public void setReversalReference(String reversalReference) { this.reversalReference = reversalReference; }

    public OffsetDateTime getSourceTimestamp() { return sourceTimestamp; }
    public void setSourceTimestamp(OffsetDateTime sourceTimestamp) { this.sourceTimestamp = sourceTimestamp; }

    public OffsetDateTime getReceivedTimestamp() { return receivedTimestamp; }
    public void setReceivedTimestamp(OffsetDateTime receivedTimestamp) { this.receivedTimestamp = receivedTimestamp; }

    public OffsetDateTime getCutoffTimestamp() { return cutoffTimestamp; }
    public void setCutoffTimestamp(OffsetDateTime cutoffTimestamp) { this.cutoffTimestamp = cutoffTimestamp; }

    /**
     * Convenience accessor for raw record ID, used in lineage tracing.
     */
    public String getRawRecordId() {
        return rawRecord != null ? rawRecord.getRecordId() : null;
    }

    /**
     * Direct accessor for batch ID through the raw record relationship.
     */
    public String getBatchId() {
        return rawRecord != null && rawRecord.getBatch() != null
                ? rawRecord.getBatch().getBatchId() : null;
    }
}
