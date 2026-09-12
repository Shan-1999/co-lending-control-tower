package com.vivriti.controltower.exception;

import com.vivriti.controltower.audit.AuditService;
import com.vivriti.controltower.canonical.ReconciliationExceptionEntity;
import com.vivriti.controltower.canonical.ReconciliationExceptionRepository;
import com.vivriti.controltower.common.enums.ExceptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing reconciliation exceptions with full audit trail.
 * All mutations are recorded via AuditService for traceability.
 * Uses optimistic locking (@Version) on the entity to prevent concurrent update conflicts.
 */
@Service
public class ExceptionService {

    private final ReconciliationExceptionRepository repository;
    private final AuditService auditService;

    public ExceptionService(ReconciliationExceptionRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public List<ReconciliationExceptionEntity> findAll() {
        return repository.findAll();
    }

    public Optional<ReconciliationExceptionEntity> findById(String exceptionId) {
        return repository.findById(exceptionId);
    }

    public List<ReconciliationExceptionEntity> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    public List<ReconciliationExceptionEntity> findByPartnerCode(String partnerCode) {
        return repository.findByPartnerCode(partnerCode);
    }

    public List<ReconciliationExceptionEntity> findByPriority(String priority) {
        return repository.findByPriority(priority);
    }

    public List<ReconciliationExceptionEntity> findByBusinessEventId(String businessEventId) {
        return repository.findByBusinessEventId(businessEventId);
    }

    @Transactional
    public ReconciliationExceptionEntity updateStatus(String exceptionId, String newStatus, String actorId) {
        ReconciliationExceptionEntity ex = repository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("Exception not found: " + exceptionId));

        String oldStatus = ex.getStatus();
        ex.setStatus(newStatus);
        ex.setActorId(actorId);
        ReconciliationExceptionEntity saved = repository.save(ex);

        auditService.log("ReconciliationException", exceptionId, "UPDATE_STATUS", actorId,
                         "{\"status\":\"" + oldStatus + "\"}",
                         "{\"status\":\"" + newStatus + "\"}",
                         "Status transition", "v1.0");

        return saved;
    }

    @Transactional
    public ReconciliationExceptionEntity override(String exceptionId, String overrideReason, String actorId) {
        ReconciliationExceptionEntity ex = repository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("Exception not found: " + exceptionId));

        String oldStatus = ex.getStatus();
        ex.setStatus(ExceptionStatus.OVERRIDDEN.name());
        ex.setOverrideReason(overrideReason);
        ex.setActorId(actorId);
        ReconciliationExceptionEntity saved = repository.save(ex);

        auditService.log("ReconciliationException", exceptionId, "OVERRIDE", actorId,
                         "{\"status\":\"" + oldStatus + "\"}",
                         "{\"status\":\"OVERRIDDEN\",\"overrideReason\":\"" + overrideReason + "\"}",
                         overrideReason, "v1.0");

        return saved;
    }

    /**
     * Returns aggregate summary of exceptions by status, priority, and classification.
     */
    public Map<String, Object> getSummary() {
        List<ReconciliationExceptionEntity> all = repository.findAll();

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(ReconciliationExceptionEntity::getStatus, Collectors.counting()));

        Map<String, Long> byPriority = all.stream()
                .collect(Collectors.groupingBy(ReconciliationExceptionEntity::getPriority, Collectors.counting()));

        Map<String, Long> byClassification = all.stream()
                .collect(Collectors.groupingBy(ReconciliationExceptionEntity::getClassification, Collectors.counting()));

        long totalExposurePaise = all.stream()
                .mapToLong(ReconciliationExceptionEntity::getExposureAmountPaise)
                .sum();

        long openCount = all.stream()
                .filter(e -> ExceptionStatus.OPEN.name().equals(e.getStatus()))
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalExceptions", all.size());
        summary.put("openExceptions", openCount);
        summary.put("totalExposurePaise", totalExposurePaise);
        summary.put("byStatus", byStatus);
        summary.put("byPriority", byPriority);
        summary.put("byClassification", byClassification);

        return summary;
    }
}
