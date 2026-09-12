package com.vivriti.controltower.close;

public record CloseEquationResult(
    long openingPositionPaise,
    long validMovementsPaise,
    long reversalsPaise,
    long closingPositionPaise,
    long matchedPaise,
    long timingPendingPaise,
    long unresolvedExceptionPaise,
    long quarantinedPaise,
    long unaccountedPaise,
    boolean isBalanced
) {}
