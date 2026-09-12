package com.vivriti.controltower.reconciliation;

public record ReconciliationSummary(
    int totalLoans,
    int exactMatches,
    int compositeMatches,
    int timingMatches,
    int probableMatches,
    int unresolvedBreaks,
    long totalMatchedPaise,
    long totalUnresolvedPaise
) {}
