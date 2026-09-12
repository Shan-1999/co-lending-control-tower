package com.vivriti.controltower.reconciliation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.canonical.MatchDecisionEntity;
import com.vivriti.controltower.canonical.MatchDecisionRepository;
import com.vivriti.controltower.common.IdGenerator;
import com.vivriti.controltower.reconciliation.matcher.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationEngine {

    private final List<MatcherStrategy> matchers;
    private final MatchDecisionRepository matchDecisionRepository;
    private final ObjectMapper objectMapper;

    public ReconciliationEngine(ExactMatcher exactMatcher,
                                CompositeMatcher compositeMatcher,
                                TimingMatcher timingMatcher,
                                ScoredProbableMatcher scoredProbableMatcher,
                                UnresolvedHandler unresolvedHandler,
                                MatchDecisionRepository matchDecisionRepository,
                                ObjectMapper objectMapper) {
        this.matchers = List.of(exactMatcher, compositeMatcher, timingMatcher, scoredProbableMatcher, unresolvedHandler);
        this.matchDecisionRepository = matchDecisionRepository;
        this.objectMapper = objectMapper;
    }

    public MatchDecisionEntity reconcile(String loanReference, List<CanonicalEventEntity> events, String ruleVersion) {
        String businessEventId = events.isEmpty() ? null : events.get(0).getBusinessEventId();

        // Idempotency guard: skip if already reconciled for this rule version
        if (businessEventId != null && matchDecisionRepository.existsByBusinessEventIdAndRuleVersion(businessEventId, ruleVersion)) {
            return matchDecisionRepository.findByBusinessEventId(businessEventId).get(0);
        }

        for (MatcherStrategy matcher : matchers) {
            Optional<MatchResult> resultOpt = matcher.match(loanReference, events, ruleVersion);
            if (resultOpt.isPresent()) {
                MatchResult result = resultOpt.get();

                MatchDecisionEntity decision = new MatchDecisionEntity();
                decision.setDecisionId(IdGenerator.newId());
                decision.setBusinessEventId(businessEventId);
                decision.setRuleVersion(ruleVersion);
                decision.setMatchLevel(result.matchLevel().name());
                decision.setConfidenceScore(result.confidenceScore());
                decision.setMatchedEventIds(result.matchedEventIds() != null ? result.matchedEventIds() : List.of());
                try {
                    decision.setEvidencePayload(objectMapper.writeValueAsString(result.evidencePayload()));
                } catch (JsonProcessingException e) {
                    decision.setEvidencePayload("{}");
                }
                decision.setDecidedAt(OffsetDateTime.now());
                try {
                    matchDecisionRepository.save(decision);
                } catch (Exception e) {
                    if (businessEventId != null) {
                        List<MatchDecisionEntity> existing = matchDecisionRepository.findByBusinessEventId(businessEventId);
                        if (!existing.isEmpty()) {
                            return existing.get(0);
                        }
                    }
                }
                return decision;
            }
        }
        return null;
    }
}
