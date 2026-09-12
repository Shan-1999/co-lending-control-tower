package com.vivriti.controltower.reconciliation;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.canonical.CanonicalEventRepository;
import com.vivriti.controltower.canonical.MatchDecisionEntity;
import com.vivriti.controltower.common.enums.MatchLevel;
import com.vivriti.controltower.exception.ExceptionRouter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final CanonicalEventRepository canonicalRepository;
    private final ReconciliationEngine engine;
    private final ExceptionRouter exceptionRouter;

    public ReconciliationService(CanonicalEventRepository canonicalRepository,
                                 ReconciliationEngine engine,
                                 ExceptionRouter exceptionRouter) {
        this.canonicalRepository = canonicalRepository;
        this.engine = engine;
        this.exceptionRouter = exceptionRouter;
    }

    public synchronized ReconciliationSummary reconcileAll() {
        List<CanonicalEventEntity> allEvents = canonicalRepository.findAll();
        Map<String, List<CanonicalEventEntity>> byLoan = allEvents.stream()
                .filter(e -> e.getLoanReference() != null)
                .collect(Collectors.groupingBy(CanonicalEventEntity::getLoanReference));

        int exact = 0, composite = 0, timing = 0, probable = 0, unresolved = 0;
        long matchedPaise = 0, unresolvedPaise = 0;

        for (Map.Entry<String, List<CanonicalEventEntity>> entry : byLoan.entrySet()) {
            String loanRef = entry.getKey();
            List<CanonicalEventEntity> events = entry.getValue();

            MatchDecisionEntity decision = engine.reconcile(loanRef, events, "v1.0");

            if (decision == null) continue;

            String level = decision.getMatchLevel();
            long amount = events.stream().mapToLong(CanonicalEventEntity::getAmountPaise).max().orElse(0);
            String partnerCode = events.stream()
                    .map(CanonicalEventEntity::getPartnerCode)
                    .filter(p -> p != null && !p.isBlank())
                    .findFirst().orElse("UNKNOWN");

            if (MatchLevel.EXACT.name().equals(level)) {
                exact++;
                matchedPaise += amount;
            } else if (MatchLevel.COMPOSITE.name().equals(level)) {
                composite++;
                matchedPaise += amount;
            } else if (MatchLevel.TIMING.name().equals(level)) {
                timing++;
                matchedPaise += amount;
            } else if (MatchLevel.PROBABLE.name().equals(level)) {
                probable++;
                unresolvedPaise += amount;
                exceptionRouter.route(loanRef, events, decision, partnerCode);
            } else if (MatchLevel.UNRESOLVED.name().equals(level)) {
                unresolved++;
                unresolvedPaise += amount;
                exceptionRouter.route(loanRef, events, decision, partnerCode);
            }
        }

        return new ReconciliationSummary(byLoan.size(), exact, composite, timing, probable, unresolved, matchedPaise, unresolvedPaise);
    }

    /**
     * Reconcile a single loan by its reference. Used by the REST API for targeted queries.
     */
    public ReconciliationSummary reconcileLoan(String loanReference) {
        List<CanonicalEventEntity> events = canonicalRepository.findByLoanReference(loanReference);
        if (events.isEmpty()) {
            return new ReconciliationSummary(0, 0, 0, 0, 0, 0, 0L, 0L);
        }

        MatchDecisionEntity decision = engine.reconcile(loanReference, events, "v1.0");
        if (decision == null) {
            return new ReconciliationSummary(1, 0, 0, 0, 0, 1, 0L, 0L);
        }

        String level = decision.getMatchLevel();
        long amount = events.stream().mapToLong(CanonicalEventEntity::getAmountPaise).max().orElse(0);
        int exact = 0, composite = 0, timing = 0, probable = 0, unresolved = 0;
        long matchedPaise = 0, unresolvedPaise = 0;

        switch (level) {
            case "EXACT" -> { exact = 1; matchedPaise = amount; }
            case "COMPOSITE" -> { composite = 1; matchedPaise = amount; }
            case "TIMING" -> { timing = 1; matchedPaise = amount; }
            case "PROBABLE" -> { probable = 1; unresolvedPaise = amount; }
            default -> { unresolved = 1; unresolvedPaise = amount; }
        }

        return new ReconciliationSummary(1, exact, composite, timing, probable, unresolved, matchedPaise, unresolvedPaise);
    }
}
