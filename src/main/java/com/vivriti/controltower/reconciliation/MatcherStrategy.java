package com.vivriti.controltower.reconciliation;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.common.enums.MatchLevel;

import java.util.List;
import java.util.Optional;

public interface MatcherStrategy {
    Optional<MatchResult> match(String loanReference, List<CanonicalEventEntity> events, String ruleVersion);
    MatchLevel getLevel();
}
