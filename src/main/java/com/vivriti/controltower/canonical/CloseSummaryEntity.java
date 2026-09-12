package com.vivriti.controltower.canonical;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "close_summary")
public class CloseSummaryEntity {

    @Id
    @Column(name = "close_id", length = 64)
    private String closeId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SourceBatchEntity batch;

    @Column(name = "business_date")
    private LocalDate businessDate;

    @Column(name = "decision", length = 32)
    private String decision;

    @Column(name = "opening_position_paise")
    private long openingPositionPaise;

    @Column(name = "valid_movements_paise")
    private long validMovementsPaise;

    @Column(name = "reversals_paise")
    private long reversalsPaise;

    @Column(name = "closing_position_paise")
    private long closingPositionPaise;

    @Column(name = "matched_paise")
    private long matchedPaise;

    @Column(name = "timing_pending_paise")
    private long timingPendingPaise;

    @Column(name = "unresolved_exception_paise")
    private long unresolvedExceptionPaise;

    @Column(name = "quarantined_paise")
    private long quarantinedPaise;

    @Column(name = "unaccounted_paise")
    private long unaccountedPaise;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "blocking_exceptions", columnDefinition = "jsonb")
    private String blockingExceptions;

    @Column(name = "decided_by", length = 64)
    private String decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    public String getCloseId() { return closeId; }
    public void setCloseId(String closeId) { this.closeId = closeId; }

    public SourceBatchEntity getBatch() { return batch; }
    public void setBatch(SourceBatchEntity batch) { this.batch = batch; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public long getOpeningPositionPaise() { return openingPositionPaise; }
    public void setOpeningPositionPaise(long openingPositionPaise) { this.openingPositionPaise = openingPositionPaise; }

    public long getValidMovementsPaise() { return validMovementsPaise; }
    public void setValidMovementsPaise(long validMovementsPaise) { this.validMovementsPaise = validMovementsPaise; }

    public long getReversalsPaise() { return reversalsPaise; }
    public void setReversalsPaise(long reversalsPaise) { this.reversalsPaise = reversalsPaise; }

    public long getClosingPositionPaise() { return closingPositionPaise; }
    public void setClosingPositionPaise(long closingPositionPaise) { this.closingPositionPaise = closingPositionPaise; }

    public long getMatchedPaise() { return matchedPaise; }
    public void setMatchedPaise(long matchedPaise) { this.matchedPaise = matchedPaise; }

    public long getTimingPendingPaise() { return timingPendingPaise; }
    public void setTimingPendingPaise(long timingPendingPaise) { this.timingPendingPaise = timingPendingPaise; }

    public long getUnresolvedExceptionPaise() { return unresolvedExceptionPaise; }
    public void setUnresolvedExceptionPaise(long unresolvedExceptionPaise) { this.unresolvedExceptionPaise = unresolvedExceptionPaise; }

    public long getQuarantinedPaise() { return quarantinedPaise; }
    public void setQuarantinedPaise(long quarantinedPaise) { this.quarantinedPaise = quarantinedPaise; }

    public long getUnaccountedPaise() { return unaccountedPaise; }
    public void setUnaccountedPaise(long unaccountedPaise) { this.unaccountedPaise = unaccountedPaise; }

    public String getBlockingExceptions() { return blockingExceptions; }
    public void setBlockingExceptions(String blockingExceptions) { this.blockingExceptions = blockingExceptions; }

    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }

    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
}
