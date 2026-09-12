package com.vivriti.controltower.canonical;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @Column(name = "log_id", length = 64)
    private String logId;

    @Column(name = "entity_name", length = 64)
    private String entityName;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "action", length = 64)
    private String action;

    @Column(name = "actor_id", length = 64)
    private String actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", columnDefinition = "jsonb")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", columnDefinition = "jsonb")
    private String afterState;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "rule_version", length = 32)
    private String ruleVersion;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }

    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
