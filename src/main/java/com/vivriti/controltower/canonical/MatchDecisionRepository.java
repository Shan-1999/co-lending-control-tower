package com.vivriti.controltower.canonical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchDecisionRepository extends JpaRepository<MatchDecisionEntity, String> {
    List<MatchDecisionEntity> findByBusinessEventId(String businessEventId);
    boolean existsByBusinessEventIdAndRuleVersion(String businessEventId, String ruleVersion);
    List<MatchDecisionEntity> findByMatchLevel(String matchLevel);
}
