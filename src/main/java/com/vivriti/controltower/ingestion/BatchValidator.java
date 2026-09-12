package com.vivriti.controltower.ingestion;

import org.springframework.stereotype.Component;

@Component
public class BatchValidator {

    public ValidationResult validate(ParsedBatch batch) {
        long actualSum = batch.records().stream().mapToLong(ParsedFeedRecord::amountPaise).sum();
        int actualCount = batch.records().size();

        boolean isValid = (actualSum == batch.declaredAmountPaise()) && (actualCount == batch.declaredCount());
        
        return new ValidationResult(isValid, actualSum - batch.declaredAmountPaise(), actualCount - batch.declaredCount());
    }

    public record ValidationResult(boolean isValid, long driftAmount, int driftCount) {}
}
