package com.vivriti.controltower.exception;

import com.vivriti.controltower.common.enums.ExceptionPriority;
import org.springframework.stereotype.Component;

/**
 * Calculates priority using threshold heuristics and integrates with Phase 2 {@link DynamicPriorityScorer}.
 */
@Component
public class PriorityCalculator {

    private final DynamicPriorityScorer dynamicScorer;

    public PriorityCalculator(DynamicPriorityScorer dynamicScorer) {
        this.dynamicScorer = dynamicScorer;
    }

    public ExceptionPriority calculate(long exposureAmountPaise, int slaHours, boolean isControlTotalMismatch) {
        if (exposureAmountPaise > 1000000L || slaHours < 1 || isControlTotalMismatch) {
            return ExceptionPriority.CRITICAL;
        } else if (exposureAmountPaise > 100000L || slaHours < 4) {
            return ExceptionPriority.HIGH;
        } else if (exposureAmountPaise > 10000L) {
            return ExceptionPriority.MEDIUM;
        } else {
            return ExceptionPriority.LOW;
        }
    }

    public ExceptionPriority calculate(long exposureAmountPaise, int slaHours, boolean isControlTotalMismatch, int recurrenceCount) {
        return dynamicScorer.score(exposureAmountPaise, slaHours, isControlTotalMismatch, recurrenceCount).priority();
    }

    public DynamicPriorityScorer.PriorityScoreResult calculateWithBreakdown(long exposureAmountPaise, int slaHours, boolean isControlTotalMismatch, int recurrenceCount) {
        return dynamicScorer.score(exposureAmountPaise, slaHours, isControlTotalMismatch, recurrenceCount);
    }
}
