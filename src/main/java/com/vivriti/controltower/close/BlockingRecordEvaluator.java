package com.vivriti.controltower.close;

import com.vivriti.controltower.canonical.ReconciliationExceptionEntity;
import com.vivriti.controltower.canonical.ReconciliationExceptionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BlockingRecordEvaluator {

    private final ReconciliationExceptionRepository exceptionRepository;

    public BlockingRecordEvaluator(ReconciliationExceptionRepository exceptionRepository) {
        this.exceptionRepository = exceptionRepository;
    }

    public List<ReconciliationExceptionEntity> evaluate(String batchId) {
        // Ideally we fetch exceptions by batchId, assuming all OPEN/IN_PROGRESS are blocking
        return exceptionRepository.findAll().stream()
                .filter(e -> "OPEN".equals(e.getStatus()) || "IN_PROGRESS".equals(e.getStatus()))
                .collect(Collectors.toList());
    }
}
