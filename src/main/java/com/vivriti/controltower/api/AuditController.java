package com.vivriti.controltower.api;

import com.vivriti.controltower.canonical.*;
import com.vivriti.controltower.common.MoneyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for audit trail queries and lineage tracing.
 * Provides full traceability from match decision back to raw payload and SHA-256 hash.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit & Lineage", description = "Audit trail and end-to-end lineage tracing")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final CanonicalEventRepository canonicalEventRepository;
    private final RawSourceRecordRepository rawSourceRecordRepository;
    private final MatchDecisionRepository matchDecisionRepository;

    public AuditController(AuditLogRepository auditLogRepository,
                           CanonicalEventRepository canonicalEventRepository,
                           RawSourceRecordRepository rawSourceRecordRepository,
                           MatchDecisionRepository matchDecisionRepository) {
        this.auditLogRepository = auditLogRepository;
        this.canonicalEventRepository = canonicalEventRepository;
        this.rawSourceRecordRepository = rawSourceRecordRepository;
        this.matchDecisionRepository = matchDecisionRepository;
    }

    @GetMapping("/logs")
    @Operation(summary = "List audit logs",
               description = "Returns audit log entries, optionally filtered by entity name and entity ID")
    public ResponseEntity<List<AuditLogEntity>> getLogs(
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String entityId) {

        if (entityName != null && entityId != null) {
            return ResponseEntity.ok(
                    auditLogRepository.findByEntityNameAndEntityId(entityName, entityId));
        }
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/trace/{loanReference}")
    @Operation(summary = "Trace full lineage for a loan reference",
               description = "Returns end-to-end lineage: raw payloads → canonical events → match decisions → " +
                             "exceptions, with SHA-256 hashes at each level. This is the evidence chain " +
                             "proving how a reconciliation decision was made.")
    public ResponseEntity<Map<String, Object>> traceLineage(
            @PathVariable String loanReference) {

        // 1. Find canonical events for this loan
        List<CanonicalEventEntity> canonicalEvents =
                canonicalEventRepository.findByLoanReference(loanReference);

        if (canonicalEvents.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 2. Trace back to raw records with SHA-256 hashes
        List<Map<String, Object>> rawRecords = canonicalEvents.stream()
                .map(event -> {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("eventId", event.getEventId());
                    record.put("sourceSystem", event.getSourceSystem());
                    record.put("eventType", event.getEventType());
                    record.put("amountPaise", event.getAmountPaise());
                    record.put("amountInr", MoneyUtils.formatPaiseAsInr(event.getAmountPaise()));
                    record.put("canonicalStatus", event.getCanonicalStatus());
                    record.put("sourceTimestamp", event.getSourceTimestamp());

                    rawSourceRecordRepository.findById(event.getRawRecordId())
                            .ifPresent(raw -> {
                                record.put("rawRecordId", raw.getRecordId());
                                record.put("payloadHash", raw.getPayloadHash());
                                record.put("rawPayload", raw.getRawPayload());
                                record.put("quarantined", raw.isQuarantined());
                                record.put("batchId", raw.getBatchId());
                            });

                    return record;
                })
                .toList();

        // 3. Find match decisions
        List<MatchDecisionEntity> matchDecisions = canonicalEvents.stream()
                .flatMap(event -> matchDecisionRepository
                        .findByBusinessEventId(event.getBusinessEventId()).stream())
                .distinct()
                .toList();

        // 4. Build lineage response
        Map<String, Object> lineage = new LinkedHashMap<>();
        lineage.put("loanReference", loanReference);
        lineage.put("totalEvents", canonicalEvents.size());
        lineage.put("canonicalEvents", rawRecords);
        lineage.put("matchDecisions", matchDecisions);

        // 5. Include audit trail for this loan's events
        List<AuditLogEntity> auditEntries = canonicalEvents.stream()
                .flatMap(event -> auditLogRepository
                        .findByEntityNameAndEntityId("MatchDecision", event.getBusinessEventId())
                        .stream())
                .toList();
        lineage.put("auditTrail", auditEntries);

        return ResponseEntity.ok(lineage);
    }
}
