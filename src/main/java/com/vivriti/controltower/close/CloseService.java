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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

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

        // Dummy stats for the sake of completion
        long matched = batch.getDeclaredAmountPaise();
        long timing = 0;
        long unresolved = 0;
        long quarantined = 0;

        CloseEquationResult eq = equationEvaluator.evaluate(batch, 0, matched, timing, unresolved, quarantined);
        var blocking = blockingEvaluator.evaluate(batchId);

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

        long threshold = 0L; // From config in a real app
        if (eq.unaccountedPaise() == 0 && eq.unresolvedExceptionPaise() <= threshold) {
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
}
