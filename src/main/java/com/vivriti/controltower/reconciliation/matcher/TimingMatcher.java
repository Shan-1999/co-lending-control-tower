package com.vivriti.controltower.reconciliation.matcher;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.common.enums.MatchLevel;
import com.vivriti.controltower.reconciliation.MatchResult;
import com.vivriti.controltower.reconciliation.MatcherStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TimingMatcher implements MatcherStrategy {

    // Configurable grace window, using 24h as requested
    private static final long GRACE_WINDOW_HOURS = 24;

    @Override
    public MatchLevel getLevel() {
        return MatchLevel.TIMING;
    }

    @Override
    public Optional<MatchResult> match(String loanReference, List<CanonicalEventEntity> events, String ruleVersion) {
        Map<String, List<CanonicalEventEntity>> bySource = events.stream()
                .collect(Collectors.groupingBy(CanonicalEventEntity::getSourceSystem));

        List<CanonicalEventEntity> originator = bySource.get("ORIGINATOR");
        List<CanonicalEventEntity> bank = bySource.get("BANK");

        if (originator != null && originator.size() == 1 && bank != null && bank.size() == 1) {
            CanonicalEventEntity origEvt = originator.get(0);
            CanonicalEventEntity bankEvt = bank.get(0);

            if (origEvt.getAmountPaise() == bankEvt.getAmountPaise() && origEvt.getCutoffTimestamp() != null) {
                if (bankEvt.getSourceTimestamp().isAfter(origEvt.getCutoffTimestamp())) {
                    long hoursDelta = Duration.between(origEvt.getCutoffTimestamp(), bankEvt.getSourceTimestamp()).toHours();
                    if (hoursDelta <= GRACE_WINDOW_HOURS) {
                        List<String> eventIds = List.of(origEvt.getEventId(), bankEvt.getEventId());
                        Map<String, Object> evidence = Map.of(
                                "timeDeltaHours", hoursDelta,
                                "cutoff", origEvt.getCutoffTimestamp().toString(),
                                "graceWindow", GRACE_WINDOW_HOURS
                        );
                        return Optional.of(new MatchResult(getLevel(), eventIds, new BigDecimal("0.9000"), evidence));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
