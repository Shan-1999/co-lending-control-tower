package com.vivriti.controltower.ingestion;

import java.time.LocalDate;
import java.util.List;

public record ParsedBatch(
    String batchId,
    String partnerCode,
    String sourceSystem,
    LocalDate businessDate,
    int declaredCount,
    long declaredAmountPaise,
    List<ParsedFeedRecord> records
) {}
