package com.vivriti.controltower.generator.groundtruth;

public record GroundTruthRecord(
        String loanReference,
        String expectedMatchLevel,
        String anomalyType,
        String expectedExceptionClassification,
        long originatorAmountPaise,
        Long bankAmountPaise,
        Long lmsAmountPaise,
        boolean allLegsPresent
) {
}
