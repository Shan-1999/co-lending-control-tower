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
public class UnresolvedHandler implements MatcherStrategy {

    @Override
    public MatchLevel getLevel() {
        return MatchLevel.UNRESOLVED;
    }

    @Override
    public Optional<MatchResult> match(String loanReference, List<CanonicalEventEntity> events, String ruleVersion) {
        List<String> presentSystems = events.stream()
                .map(CanonicalEventEntity::getSourceSystem)
                .distinct()
                .collect(Collectors.toList());

        List<String> expectedSystems = List.of("ORIGINATOR", "BANK", "LMS");
        List<String> missing = expectedSystems.stream()
                .filter(s -> !presentSystems.contains(s))
                .collect(Collectors.toList());

        Map<String, Object> evidence = Map.of(
                "reason", "Missing legs or no match",
                "missingLegs", missing
        );
        List<String> eventIds = events.stream().map(CanonicalEventEntity::getEventId).collect(Collectors.toList());

        return Optional.of(new MatchResult(getLevel(), eventIds, BigDecimal.ZERO, evidence));
    }
}
