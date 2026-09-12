package com.vivriti.controltower.reconciliation;

import com.vivriti.controltower.common.enums.MatchLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MatchResult(
    MatchLevel matchLevel,
    List<String> matchedEventIds,
    BigDecimal confidenceScore,
    Map<String, Object> evidencePayload
) {}
