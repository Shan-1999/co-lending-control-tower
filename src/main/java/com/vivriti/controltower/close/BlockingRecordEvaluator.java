package com.vivriti.controltower.close;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.canonical.CanonicalEventRepository;
import com.vivriti.controltower.canonical.RawSourceRecordEntity;
import com.vivriti.controltower.canonical.RawSourceRecordRepository;
import com.vivriti.controltower.canonical.ReconciliationExceptionEntity;
import com.vivriti.controltower.canonical.ReconciliationExceptionRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BlockingRecordEvaluator {

    private final ReconciliationExceptionRepository exceptionRepository;
    private final CanonicalEventRepository canonicalRepository;
    private final RawSourceRecordRepository rawRepository;

    public BlockingRecordEvaluator(ReconciliationExceptionRepository exceptionRepository,
                                  CanonicalEventRepository canonicalRepository,
                                  RawSourceRecordRepository rawRepository) {
        this.exceptionRepository = exceptionRepository;
        this.canonicalRepository = canonicalRepository;
        this.rawRepository = rawRepository;
    }

    public List<ReconciliationExceptionEntity> evaluate(String batchId) {
        List<CanonicalEventEntity> events = canonicalRepository.findByBatchId(batchId);

        Set<String> bizIds = new HashSet<>();
        Set<String> loanRefs = new HashSet<>();

        for (CanonicalEventEntity evt : events) {
            if (evt.getBusinessEventId() != null) bizIds.add(evt.getBusinessEventId());
            if (evt.getLoanReference() != null) loanRefs.add(evt.getLoanReference());
        }

        if (bizIds.isEmpty() && loanRefs.isEmpty()) {
            List<RawSourceRecordEntity> raws = rawRepository.findByBatchId(batchId);
            for (RawSourceRecordEntity raw : raws) {
                if (raw.getRawPayload() != null) {
                    int idx = raw.getRawPayload().indexOf("LOAN-");
                    if (idx >= 0 && idx + 11 <= raw.getRawPayload().length()) {
                        loanRefs.add(raw.getRawPayload().substring(idx, idx + 11));
                    }
                }
            }
        }

        List<ReconciliationExceptionEntity> allOpen = exceptionRepository.findAll().stream()
                .filter(e -> "OPEN".equals(e.getStatus()) || "IN_PROGRESS".equals(e.getStatus()))
                .collect(Collectors.toList());

        if (bizIds.isEmpty() && loanRefs.isEmpty()) {
            return allOpen;
        }

        return allOpen.stream()
                .filter(e -> bizIds.contains(e.getBusinessEventId()) ||
                             (e.getBusinessEventId() != null && loanRefs.stream().anyMatch(ref -> e.getBusinessEventId().contains(ref))))
                .collect(Collectors.toList());
    }
}
