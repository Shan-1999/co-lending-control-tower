package com.vivriti.controltower.ingestion;

import java.time.OffsetDateTime;

public record ParsedFeedRecord(
    String loanReference,
    long amountPaise,
    String status,
    OffsetDateTime timestamp,
    String utrReference,
    String eventType,
    String correlationId,
    String businessEventId,
    String partnerCode,
    String sourceSystem,
    String reversalReference,
    String rawPayload
) {}
