package com.vivriti.controltower.generator;

import com.vivriti.controltower.common.enums.CanonicalStatus;
import com.vivriti.controltower.common.enums.EventType;
import com.vivriti.controltower.common.enums.SourceSystem;

import java.time.OffsetDateTime;

public record FeedRecord(
        String loanReference,
        long amount,
        CanonicalStatus status,
        OffsetDateTime timestamp,
        String utrReference,
        String partnerCode,
        SourceSystem sourceSystem,
        EventType eventType,
        String reversalReference,
        String correlationId,
        String businessEventId
) {
}
