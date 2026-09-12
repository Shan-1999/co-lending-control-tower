package com.vivriti.controltower.reconciliation.matcher;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.common.enums.MatchLevel;
import com.vivriti.controltower.reconciliation.MatchResult;
import com.vivriti.controltower.reconciliation.MatcherStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CompositeMatcher implements MatcherStrategy {

    @Override
    public MatchLevel getLevel() {
        return MatchLevel.COMPOSITE;
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
            bank != null && bank.size() > 1 &&
            lms != null && lms.size() == 1) {
            
            long amtOrig = originator.get(0).getAmountPaise();
            long amtLms = lms.get(0).getAmountPaise();
            long sumBank = bank.stream().mapToLong(CanonicalEventEntity::getAmountPaise).sum();

            if (amtOrig == sumBank && amtOrig == amtLms) {
                List<String> eventIds = new ArrayList<>();
                eventIds.add(originator.get(0).getEventId());
                eventIds.add(lms.get(0).getEventId());
                bank.forEach(b -> eventIds.add(b.getEventId()));

                Map<String, Object> evidence = Map.of(
                        "amount", amtOrig,
                        "sumBank", sumBank,
                        "bankParts", bank.stream().map(CanonicalEventEntity::getAmountPaise).collect(Collectors.toList()),
                        "matchedIds", eventIds
                );
                return Optional.of(new MatchResult(getLevel(), eventIds, new BigDecimal("0.9500"), evidence));
            }
        }
        return Optional.empty();
    }
}
