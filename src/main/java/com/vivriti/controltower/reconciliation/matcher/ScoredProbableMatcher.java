package com.vivriti.controltower.reconciliation.matcher;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.common.enums.MatchLevel;
import com.vivriti.controltower.config.ControlTowerProperties;
import com.vivriti.controltower.reconciliation.MatchResult;
import com.vivriti.controltower.reconciliation.MatcherStrategy;
import com.vivriti.controltower.reconciliation.ProbableMatchProposal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Level 4 Matcher (Phase 2 Intelligence): Scored Probable Matcher.
 *
 * <p>READ-ONLY ADVISORY matching based on weighted heuristics:
 *   S = 0.40 * AmountScore + 0.25 * DateScore + 0.35 * RefSimilarity
 *
 * <p>GUARDRAIL: NEVER auto-reconciles. If S >= 0.82, it emits a {@link ProbableMatchProposal}
 * with requiresHumanApproval = true. The underlying event remains UNRESOLVED until
 * an authorized operator confirms or rejects the proposal.</p>
 */
@Component
public class ScoredProbableMatcher implements MatcherStrategy {

    private static final double THRESHOLD = 0.85;
    public static final double DEFAULT_THRESHOLD = 0.82;
    private final ControlTowerProperties properties;

    public ScoredProbableMatcher(ControlTowerProperties properties) {
        this.properties = properties;
    }

    @Override
    public MatchLevel getLevel() {
        return MatchLevel.PROBABLE;
    }

    @Override
    public Optional<MatchResult> match(String loanReference, List<CanonicalEventEntity> events, String ruleVersion) {
        if (events == null || events.size() < 2) {
            return Optional.empty();
        }

        // Pairwise evaluation across disparate feed legs (e.g. Originator vs Bank or Bank vs LMS)
        CanonicalEventEntity e1 = events.get(0);
        CanonicalEventEntity e2 = events.get(1);

        double amountScore = calculateAmountScore(e1.getAmountPaise(), e2.getAmountPaise());
        double dateScore = calculateDateScore(e1.getSourceTimestamp(), e2.getSourceTimestamp());
        double refSimilarity = calculateLevenshteinSimilarity(e1.getLoanReference(), e2.getLoanReference());

        // Configuration weights or defaults: 0.40 * Amount + 0.25 * Date + 0.35 * Ref
        double wAmount = 0.40;
        double wDate = 0.25;
        double wRef = 0.35;

        double compositeScore = (wAmount * amountScore) + (wDate * dateScore) + (wRef * refSimilarity);
        double threshold = DEFAULT_THRESHOLD;
        if (properties != null && properties.getReconciliation() != null
                && properties.getReconciliation().getProbableMatchThreshold() > 0) {
            threshold = properties.getReconciliation().getProbableMatchThreshold();
        }

        if (compositeScore >= threshold) {
            BigDecimal roundedScore = BigDecimal.valueOf(compositeScore).setScale(4, RoundingMode.HALF_UP);
            List<String> eventIds = events.stream()
                    .map(CanonicalEventEntity::getEventId)
                    .collect(Collectors.toList());

            String rationale = String.format(
                    "Probable match identified with confidence %.2f%% (AmountScore: %.2f, DateScore: %.2f, RefSimilarity: %.2f). Requires human authorization.",
                    compositeScore * 100.0, amountScore, dateScore, refSimilarity
            );

            Map<String, Object> contributingFields = new LinkedHashMap<>();
            contributingFields.put("amountScore", amountScore);
            contributingFields.put("dateScore", dateScore);
            contributingFields.put("refSimilarity", refSimilarity);
            contributingFields.put("amountE1Paise", e1.getAmountPaise());
            contributingFields.put("amountE2Paise", e2.getAmountPaise());
            contributingFields.put("refE1", e1.getLoanReference());
            contributingFields.put("refE2", e2.getLoanReference());

            ProbableMatchProposal proposal = new ProbableMatchProposal(
                    loanReference,
                    eventIds,
                    roundedScore,
                    amountScore,
                    dateScore,
                    refSimilarity,
                    true,
                    rationale,
                    contributingFields
            );

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("proposal", proposal);
            evidence.put("score", compositeScore);
            evidence.put("threshold", threshold);
            evidence.put("requiresHumanApproval", true);

            return Optional.of(new MatchResult(getLevel(), eventIds, roundedScore, evidence));
        }

        return Optional.empty();
    }

    private double calculateAmountScore(long amountA, long amountB) {
        if (amountA == amountB) return 1.0;
        double maxAmount = Math.max(amountA, amountB);
        if (maxAmount <= 0) return 0.0;
        double diff = Math.abs(amountA - amountB);
        return Math.max(0.0, 1.0 - (diff / maxAmount));
    }

    private double calculateDateScore(java.time.OffsetDateTime t1, java.time.OffsetDateTime t2) {
        if (t1 == null || t2 == null) return 0.5; // Neutral if timestamp is missing
        long hoursDelta = Math.abs(Duration.between(t1, t2).toHours());
        // Linear decay over 72 hours
        return Math.max(0.0, 1.0 - ((double) hoursDelta / 72.0));
    }

    /**
     * Normalized Levenshtein distance similarity bounded in [0.0, 1.0].
     */
    public static double calculateLevenshteinSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int distance = computeLevenshteinDistance(s1, s2);
        return Math.max(0.0, 1.0 - ((double) distance / maxLen));
    }

    private static int computeLevenshteinDistance(CharSequence s1, CharSequence s2) {
        int len0 = s1.length() + 1;
        int len1 = s2.length() + 1;
        int[] cost = new int[len0];
        int[] newCost = new int[len0];

        for (int i = 0; i < len0; i++) cost[i] = i;

        for (int j = 1; j < len1; j++) {
            newCost[0] = j;
            for (int i = 1; i < len0; i++) {
                int match = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                int costReplace = cost[i - 1] + match;
                int costInsert = cost[i] + 1;
                int costDelete = newCost[i - 1] + 1;
                newCost[i] = Math.min(Math.min(costInsert, costDelete), costReplace);
            }
            int[] swap = cost;
            cost = newCost;
            newCost = swap;
        }
        return cost[len0 - 1];
    }
}
