package com.vivriti.controltower.exception;

import com.vivriti.controltower.common.MoneyUtils;
import com.vivriti.controltower.common.enums.ExceptionPriority;
import org.springframework.stereotype.Component;

/**
 * Phase 2 Intelligence: Multi-factor Dynamic Priority Scorer.
 *
 * <p>Calculates exception urgency using a transparent multi-factor model:
 * <ol>
 *   <li><b>Exposure Value</b>: Log-scale exposure in paise (0.0 to 1.0)</li>
 *   <li><b>SLA Urgency</b>: Hours remaining until SLA violation (0.0 to 1.0)</li>
 *   <li><b>Close-Blocker Status</b>: Whether the break actively blocks the daily close (1.0 or 0.0)</li>
 *   <li><b>Partner Recurrence</b>: Frequent break history from the same partner (0.0 to 1.0)</li>
 * </ol>
 *
 * <p>Categorizes breaks into CRITICAL, HIGH, MEDIUM, and LOW with an explanatory breakdown.</p>
 */
@Component
public class DynamicPriorityScorer {

    public record PriorityScoreResult(
            ExceptionPriority priority,
            double totalScore,
            String explanation
    ) {}

    public PriorityScoreResult score(long exposurePaise, int slaRemainingHours, boolean isCloseBlocker, int partnerRecurrenceCount) {
        // 1. Exposure Score (Logarithmic scaling: 10,000 paise = 0.5, 10,000,000 paise = 1.0)
        double exposureScore;
        if (exposurePaise <= 0) {
            exposureScore = 0.0;
        } else {
            // max at 1 crore (10,000,000 INR = 1,000,000,000 paise)
            double logExposure = Math.log10(exposurePaise);
            exposureScore = Math.min(1.0, Math.max(0.0, logExposure / 9.0));
        }

        // 2. SLA Urgency Score
        double slaScore;
        if (slaRemainingHours <= 1) {
            slaScore = 1.0;
        } else if (slaRemainingHours <= 4) {
            slaScore = 0.75;
        } else if (slaRemainingHours <= 12) {
            slaScore = 0.50;
        } else {
            slaScore = 0.20;
        }

        // 3. Close Blocker Score
        double blockerScore = isCloseBlocker ? 1.0 : 0.0;

        // 4. Partner Recurrence Score (Normalized up to 20 past exceptions)
        double recurrenceScore = Math.min(1.0, partnerRecurrenceCount / 20.0);

        // Weighted Total
        double totalScore = (0.35 * exposureScore)
                          + (0.25 * slaScore)
                          + (0.25 * blockerScore)
                          + (0.15 * recurrenceScore);

        ExceptionPriority priority;
        if (totalScore >= 0.70 || isCloseBlocker) {
            priority = ExceptionPriority.CRITICAL;
        } else if (totalScore >= 0.45) {
            priority = ExceptionPriority.HIGH;
        } else if (totalScore >= 0.25) {
            priority = ExceptionPriority.MEDIUM;
        } else {
            priority = ExceptionPriority.LOW;
        }

        String explanation = String.format(
                "[%s] Score: %.2f | Exposure: %s (score: %.2f) | SLA: %dh left (score: %.2f) | Blocker: %s (score: %.2f) | Recurrence: %d (score: %.2f)",
                priority.name(),
                totalScore,
                MoneyUtils.formatPaiseAsInr(exposurePaise),
                exposureScore,
                slaRemainingHours,
                slaScore,
                isCloseBlocker ? "YES" : "NO",
                blockerScore,
                partnerRecurrenceCount,
                recurrenceScore
        );

        return new PriorityScoreResult(priority, totalScore, explanation);
    }
}

