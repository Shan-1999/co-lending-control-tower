package com.vivriti.controltower.reconciliation.matcher;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.common.enums.MatchLevel;
import com.vivriti.controltower.reconciliation.MatchResult;
import com.vivriti.controltower.reconciliation.MatcherStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ExactMatcher implements MatcherStrategy {

    @Override
    public MatchLevel getLevel() {
        return MatchLevel.EXACT;
    }

    @Override
    public Optional<MatchResult> match(String loanReference, List<CanonicalEventEntity> events, String ruleVersion) {
        Map<String, List<CanonicalEventEntity>> bySource = events.stream()
                .filter(e -> "SUCCESS".equals(e.getCanonicalStatus()))
                .collect(Collectors.groupingBy(CanonicalEventEntity::getSourceSystem));

        List<CanonicalEventEntity> originator = bySource.get("ORIGINATOR");
        List<CanonicalEventEntity> bank = bySource.get("BANK");
        List<CanonicalEventEntity> lms = bySource.get("LMS");

        if (originator != null && originator.size() == 1 &&
            bank != null && bank.size() == 1 &&
            lms != null && lms.size() == 1) {
            
            long amtOrig = originator.get(0).getAmountPaise();
            long amtBank = bank.get(0).getAmountPaise();
            long amtLms = lms.get(0).getAmountPaise();

            if (amtOrig == amtBank && amtBank == amtLms) {
                // If bank timestamp is after cutoff, this is a TIMING difference, defer to TimingMatcher
                if (originator.get(0).getCutoffTimestamp() != null &&
                    bank.get(0).getSourceTimestamp() != null &&
                    bank.get(0).getSourceTimestamp().isAfter(originator.get(0).getCutoffTimestamp())) {
                    return Optional.empty();
                }

                List<String> eventIds = List.of(originator.get(0).getEventId(), bank.get(0).getEventId(), lms.get(0).getEventId());
                Map<String, Object> evidence = Map.of(
                        "amount", amtOrig,
                        "matchedIds", eventIds,
                        "statuses", "SUCCESS"
                );
                return Optional.of(new MatchResult(getLevel(), eventIds, new BigDecimal("1.0000"), evidence));
            }
        }
        return Optional.empty();
    }
}
