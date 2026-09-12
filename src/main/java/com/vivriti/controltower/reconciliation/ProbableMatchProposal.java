package com.vivriti.controltower.reconciliation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Phase 2 Intelligence: Read-only advisory probable match proposal.
 *
 * <p>Represents a proposed relationship computed from heuristic similarity
 * (Amount proximity + Date proximity + Levenshtein reference similarity).
 * Under no circumstance does this proposal auto-reconcile or alter financial state.
 * It requires human review and authorization.</p>
 */
public record ProbableMatchProposal(
        String loanReference,
        List<String> eventIds,
        BigDecimal confidenceScore,
        double amountScore,
        double dateScore,
        double refSimilarity,
        boolean requiresHumanApproval,
        String rationale,
        Map<String, Object> contributingFields
) {}

