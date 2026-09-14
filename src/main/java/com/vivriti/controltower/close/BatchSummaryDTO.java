package com.vivriti.controltower.close;

import java.time.LocalDate;

/**
 * High-level summary of an ingested batch for workbench triage and filtering.
 */
public record BatchSummaryDTO(
        String batchId,
        String partnerCode,
        String sourceSystem,
        LocalDate businessDate,
        int declaredCount,
        long declaredAmountPaise,
        String status,            // VALIDATED or QUARANTINED
        String decision,          // CLOSE or HOLD
        int blockingCount
) {}

