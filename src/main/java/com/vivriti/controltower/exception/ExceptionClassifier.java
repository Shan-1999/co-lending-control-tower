package com.vivriti.controltower.exception;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.canonical.MatchDecisionEntity;
import com.vivriti.controltower.common.enums.ExceptionClassification;
import com.vivriti.controltower.common.enums.ExceptionPriority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExceptionClassifier {

    public ClassificationResult classify(String loanReference, List<CanonicalEventEntity> events, MatchDecisionEntity matchDecision) {
        Map<String, List<CanonicalEventEntity>> bySource = events.stream()
                .collect(Collectors.groupingBy(CanonicalEventEntity::getSourceSystem));

        boolean hasOrig = bySource.containsKey("ORIGINATOR") && !bySource.get("ORIGINATOR").isEmpty();
        boolean hasBank = bySource.containsKey("BANK") && !bySource.get("BANK").isEmpty();
        boolean hasLms = bySource.containsKey("LMS") && !bySource.get("LMS").isEmpty();

        ExceptionClassification classification = ExceptionClassification.MISSING_FEED_LEG;
        String ownerRole = "PARTNER_INTEGRATION_ENG";
        int slaHours = 24;
        String recAction = "Investigate missing leg";

        if (hasOrig && hasLms && !hasBank) {
            classification = ExceptionClassification.MISSING_FEED_LEG;
            ownerRole = "PARTNER_INTEGRATION_ENG";
        } else if (hasOrig && hasBank && !hasLms) {
            classification = ExceptionClassification.MISSING_FEED_LEG;
            ownerRole = "PARTNER_INTEGRATION_ENG";
        } else if (hasOrig && hasBank && hasLms) {
            long amtOrig = bySource.get("ORIGINATOR").get(0).getAmountPaise();
            long amtBank = bySource.get("BANK").get(0).getAmountPaise();
            long amtLms = bySource.get("LMS").get(0).getAmountPaise();

            if (amtOrig != amtBank || amtOrig != amtLms) {
                classification = ExceptionClassification.AMOUNT_MISMATCH;
                ownerRole = "FINANCE_OPERATIONS";
                recAction = "Review amounts and correct discrepancy";
            } else {
                classification = ExceptionClassification.STATUS_MISMATCH;
                ownerRole = "LENDING_OPERATIONS";
                recAction = "Review status mismatch";
            }
        } else if (bySource.getOrDefault("BANK", List.of()).size() > 1 && !hasOrig) {
            classification = ExceptionClassification.DUPLICATE_CALLBACK;
            ownerRole = "BACKEND_PLATFORM_ENG";
            recAction = "Deduplicate callbacks";
        } else if (events.stream().anyMatch(e -> e.getReversalReference() != null && !hasOrig)) {
            classification = ExceptionClassification.ORPHAN_REVERSAL;
            ownerRole = "FINANCE_OPERATIONS";
            recAction = "Map orphan reversal to original transaction";
        }

        // We use low priority default here, priority calculator will override it.
        return new ClassificationResult(classification, ownerRole, ExceptionPriority.LOW, slaHours, recAction);
    }
}
