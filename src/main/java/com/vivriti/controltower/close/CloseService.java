package com.vivriti.controltower.close;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivriti.controltower.audit.AuditService;
import com.vivriti.controltower.audit.MakerCheckerGuard;
import com.vivriti.controltower.canonical.CloseSummaryEntity;
import com.vivriti.controltower.canonical.CloseSummaryRepository;
import com.vivriti.controltower.canonical.SourceBatchEntity;
import com.vivriti.controltower.canonical.SourceBatchRepository;
import com.vivriti.controltower.common.IdGenerator;
import com.vivriti.controltower.common.enums.CloseDecision;
import com.vivriti.controltower.canonical.ReconciliationExceptionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CloseService {

    private final SourceBatchRepository batchRepository;
    private final CloseSummaryRepository closeSummaryRepository;
    private final CloseEquationEvaluator equationEvaluator;
    private final BlockingRecordEvaluator blockingEvaluator;
    private final AuditService auditService;
    private final MakerCheckerGuard makerCheckerGuard;
    private final ObjectMapper objectMapper;

    public CloseService(SourceBatchRepository batchRepository,
                        CloseSummaryRepository closeSummaryRepository,
                        CloseEquationEvaluator equationEvaluator,
                        BlockingRecordEvaluator blockingEvaluator,
                        AuditService auditService,
                        MakerCheckerGuard makerCheckerGuard,
                        ObjectMapper objectMapper) {
        this.batchRepository = batchRepository;
        this.closeSummaryRepository = closeSummaryRepository;
        this.equationEvaluator = equationEvaluator;
        this.blockingEvaluator = blockingEvaluator;
        this.auditService = auditService;
        this.makerCheckerGuard = makerCheckerGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CloseSummaryEntity evaluateClose(String batchId, String actorId) {
        SourceBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        var batchBlocking = blockingEvaluator.evaluate(batchId);

        long matched = batch.getDeclaredAmountPaise();
        long timing = 0;
        long unresolved = 0;
        long quarantined = 0;

        if ("QUARANTINED".equalsIgnoreCase(batch.getStatus())) {
            quarantined = batch.getDeclaredAmountPaise();
            matched = 0;
        } else if (!batchBlocking.isEmpty()) {
            unresolved = batchBlocking.stream()
                    .mapToLong(ReconciliationExceptionEntity::getExposureAmountPaise)
                    .sum();
            unresolved = Math.min(unresolved, batch.getDeclaredAmountPaise());
            matched = Math.max(0, batch.getDeclaredAmountPaise() - unresolved);
        }

        CloseEquationResult eq = equationEvaluator.evaluate(batch, 0, matched, timing, unresolved, quarantined);
        List<ReconciliationExceptionEntity> blocking = batchBlocking;

        CloseSummaryEntity summary = new CloseSummaryEntity();
        summary.setCloseId(IdGenerator.newId());
        summary.setBatch(batch);
        summary.setBusinessDate(batch.getBusinessDate());
        summary.setOpeningPositionPaise(eq.openingPositionPaise());
        summary.setValidMovementsPaise(eq.validMovementsPaise());
        summary.setReversalsPaise(eq.reversalsPaise());
        summary.setClosingPositionPaise(eq.closingPositionPaise());
        summary.setMatchedPaise(eq.matchedPaise());
        summary.setTimingPendingPaise(eq.timingPendingPaise());
        summary.setUnresolvedExceptionPaise(eq.unresolvedExceptionPaise());
        summary.setQuarantinedPaise(eq.quarantinedPaise());
        summary.setUnaccountedPaise(eq.unaccountedPaise());

        long threshold = 0L; // Zero tolerance
        if (eq.unaccountedPaise() == 0 
                && eq.unresolvedExceptionPaise() <= threshold 
                && eq.quarantinedPaise() == 0 
                && !"QUARANTINED".equalsIgnoreCase(batch.getStatus())) {
            summary.setDecision(CloseDecision.CLOSE.name());
        } else {
            summary.setDecision(CloseDecision.HOLD.name());
            try {
                summary.setBlockingExceptions(objectMapper.writeValueAsString(blocking));
            } catch (Exception e) {
                summary.setBlockingExceptions("[]");
            }
        }
        summary.setDecidedBy(actorId);
        summary.setDecidedAt(OffsetDateTime.now());

        closeSummaryRepository.save(summary);
        auditService.log("CloseSummary", summary.getCloseId(), "EVALUATE", actorId, null, summary.getDecision(), "Close Evaluation", "v1.0");
        return summary;
    }

    @Transactional
    public CloseSummaryEntity approveClose(String closeId, String approverId) {
        // Dummy check for ROLE_APPROVER (Assume handled at controller level via security)
        CloseSummaryEntity summary = closeSummaryRepository.findById(closeId)
                .orElseThrow(() -> new IllegalArgumentException("Close not found"));

        makerCheckerGuard.assertSegregation(summary.getBatch().getBatchId(), approverId);

        summary.setDecision(CloseDecision.CLOSE.name());
        summary.setDecidedBy(approverId);
        summary.setDecidedAt(OffsetDateTime.now());

        closeSummaryRepository.save(summary);
        auditService.log("CloseSummary", closeId, "APPROVE", approverId, "HOLD", "CLOSE", "Close Approved", "v1.0");

        return summary;
    }

    @Transactional
    public int overrideBatchExceptions(String batchId, String reason, String actorId) {
        List<ReconciliationExceptionEntity> batchBlocking = blockingEvaluator.evaluate(batchId);
        int count = 0;
        for (ReconciliationExceptionEntity exc : batchBlocking) {
            exc.setStatus("OVERRIDDEN");
            exc.setOverrideReason(reason);
            exc.setActorId(actorId);
            auditService.log("ReconciliationException", exc.getExceptionId(), "OVERRIDE", actorId,
                    "OPEN", "OVERRIDDEN", reason, "v1.0");
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<BatchSummaryDTO> getBatchSummaries() {
        List<SourceBatchEntity> batches = batchRepository.findAll();
        List<BatchSummaryDTO> dtos = new java.util.ArrayList<>();
        for (SourceBatchEntity b : batches) {
            String decision;
            int blockingCount = 0;
            if ("QUARANTINED".equalsIgnoreCase(b.getStatus())) {
                decision = "HOLD";
            } else {
                List<ReconciliationExceptionEntity> blocking = blockingEvaluator.evaluate(b.getBatchId());
                blockingCount = blocking.size();
                decision = (blockingCount == 0) ? "CLOSE" : "HOLD";
            }
            dtos.add(new BatchSummaryDTO(
                    b.getBatchId(),
                    b.getPartnerCode(),
                    b.getSourceSystem(),
                    b.getBusinessDate(),
                    b.getDeclaredCount(),
                    b.getDeclaredAmountPaise(),
                    b.getStatus(),
                    decision,
                    blockingCount
            ));
        }
        return dtos;
    }
}
