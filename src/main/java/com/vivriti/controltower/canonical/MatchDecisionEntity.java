package com.vivriti.controltower.canonical;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "match_decision", uniqueConstraints = @UniqueConstraint(columnNames = {"business_event_id", "rule_version"}))
public class MatchDecisionEntity {

    @Id
    @Column(name = "decision_id", length = 64)
    private String decisionId;

    @Column(name = "business_event_id", length = 64)
    private String businessEventId;

    @Column(name = "rule_version", length = 32)
    private String ruleVersion;

    @Column(name = "match_level", length = 32)
    private String matchLevel;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "matched_event_ids", columnDefinition = "text[]")
    private List<String> matchedEventIds = new ArrayList<>();

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_payload", columnDefinition = "jsonb")
    private String evidencePayload;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }

    public String getBusinessEventId() { return businessEventId; }
    public void setBusinessEventId(String businessEventId) { this.businessEventId = businessEventId; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public List<String> getMatchedEventIds() { return matchedEventIds; }
    public void setMatchedEventIds(List<String> matchedEventIds) { this.matchedEventIds = matchedEventIds != null ? matchedEventIds : new ArrayList<>(); }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getEvidencePayload() { return evidencePayload; }
    public void setEvidencePayload(String evidencePayload) { this.evidencePayload = evidencePayload; }

    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
}
