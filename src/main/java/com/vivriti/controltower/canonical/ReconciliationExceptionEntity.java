package com.vivriti.controltower.canonical;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reconciliation_exception")
public class ReconciliationExceptionEntity {

    @Id
    @Column(name = "exception_id", length = 64)
    private String exceptionId;

    @Column(name = "business_event_id", length = 64)
    private String businessEventId;

    @Column(name = "partner_code", length = 32)
    private String partnerCode;

    @Column(name = "classification", length = 64)
    private String classification;

    @Column(name = "owner_role", length = 64)
    private String ownerRole;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "priority", length = 32)
    private String priority;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "exposure_amount_paise")
    private long exposureAmountPaise;

    @Column(name = "sla_deadline")
    private OffsetDateTime slaDeadline;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Column(name = "actor_id", length = 64)
    private String actorId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    public String getExceptionId() { return exceptionId; }
    public void setExceptionId(String exceptionId) { this.exceptionId = exceptionId; }

    public String getBusinessEventId() { return businessEventId; }
    public void setBusinessEventId(String businessEventId) { this.businessEventId = businessEventId; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getOwnerRole() { return ownerRole; }
    public void setOwnerRole(String ownerRole) { this.ownerRole = ownerRole; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getExposureAmountPaise() { return exposureAmountPaise; }
    public void setExposureAmountPaise(long exposureAmountPaise) { this.exposureAmountPaise = exposureAmountPaise; }

    public OffsetDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(OffsetDateTime slaDeadline) { this.slaDeadline = slaDeadline; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
