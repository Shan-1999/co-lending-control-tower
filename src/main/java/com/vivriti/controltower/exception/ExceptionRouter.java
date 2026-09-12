package com.vivriti.controltower.exception;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.canonical.MatchDecisionEntity;
import com.vivriti.controltower.canonical.ReconciliationExceptionEntity;
import com.vivriti.controltower.canonical.ReconciliationExceptionRepository;
import com.vivriti.controltower.common.IdGenerator;
import com.vivriti.controltower.common.enums.ExceptionClassification;
import com.vivriti.controltower.common.enums.ExceptionPriority;
import com.vivriti.controltower.common.enums.ExceptionStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Routes unresolved reconciliation breaks to the exception queue with
 * classification, ownership, SLA, and priority assignment.
 */
@Component
public class ExceptionRouter {

    private final ExceptionClassifier classifier;
    private final PriorityCalculator priorityCalculator;
    private final ReconciliationExceptionRepository repository;

    public ExceptionRouter(ExceptionClassifier classifier,
                           PriorityCalculator priorityCalculator,
                           ReconciliationExceptionRepository repository) {
        this.classifier = classifier;
        this.priorityCalculator = priorityCalculator;
        this.repository = repository;
    }

    public ReconciliationExceptionEntity route(String loanReference,
                                                List<CanonicalEventEntity> events,
                                                MatchDecisionEntity matchDecision,
                                                String partnerCode) {
        ClassificationResult result = classifier.classify(loanReference, events, matchDecision);

        long exposure = events.stream()
                .mapToLong(CanonicalEventEntity::getAmountPaise)
                .max().orElse(0L);

        boolean isControlMismatch =
                result.classification() == ExceptionClassification.CONTROL_TOTAL_MISMATCH;
        ExceptionPriority priority =
                priorityCalculator.calculate(exposure, result.slaHours(), isControlMismatch);

        ReconciliationExceptionEntity entity = new ReconciliationExceptionEntity();
        entity.setExceptionId(IdGenerator.newId());
        entity.setBusinessEventId(matchDecision.getBusinessEventId());
        entity.setPartnerCode(partnerCode);
        entity.setClassification(result.classification().name());
        entity.setOwnerRole(result.ownerRole());
        entity.setRecommendedAction(result.recommendedAction());
        entity.setPriority(priority.name());
        entity.setStatus(ExceptionStatus.OPEN.name());
        entity.setExposureAmountPaise(exposure);
        entity.setSlaDeadline(OffsetDateTime.now().plusHours(result.slaHours()));
        entity.setCreatedAt(OffsetDateTime.now());

        return repository.save(entity);
    }
}
