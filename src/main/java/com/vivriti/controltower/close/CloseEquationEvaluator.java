package com.vivriti.controltower.close;

import com.vivriti.controltower.canonical.RawSourceRecordEntity;
import com.vivriti.controltower.canonical.RawSourceRecordRepository;
import com.vivriti.controltower.canonical.SourceBatchEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CloseEquationEvaluator {

    private final RawSourceRecordRepository rawRepository;

    public CloseEquationEvaluator(RawSourceRecordRepository rawRepository) {
        this.rawRepository = rawRepository;
    }

    public CloseEquationResult evaluate(SourceBatchEntity batch, long openingPositionPaise, long matched, long timing, long unresolved, long quarantined) {
        List<RawSourceRecordEntity> raws = rawRepository.findByBatchId(batch.getBatchId());

        long totalInstructed = batch.getDeclaredAmountPaise();
        long validMovements = totalInstructed; // Simplify for now
        long reversals = 0; // Simplified
        long closingPositionPaise = openingPositionPaise + validMovements - reversals;

        long accounted = matched + timing + unresolved + quarantined;
        long unaccounted = totalInstructed - accounted;

        return new CloseEquationResult(openingPositionPaise, validMovements, reversals, closingPositionPaise, matched, timing, unresolved, quarantined, unaccounted, unaccounted == 0);
    }
}
