package com.vivriti.controltower.generator;

import com.vivriti.controltower.common.enums.CanonicalStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AnomalyDistributionTest {

    @Test
    void testAnomalyDistribution() {
        AnomalyInjector injector = new AnomalyInjector();
        
        // Generate 1000 dummy scenarios
        List<LoanScenario> scenarios = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 1; i <= 1000; i++) {
            LoanScenario scenario = new LoanScenario();
            scenario.setLoanReference(String.format("LOAN-%06d", i));
            scenario.setPartnerCode("PARTNER_ALPHA");
            scenario.setBusinessDay(1);
            
            long amount = 100000L;
            scenario.setOriginatorAmount(amount);
            scenario.setBankAmount(amount);
            scenario.setLmsAmount(amount);
            
            scenario.setOriginatorStatus(CanonicalStatus.SUCCESS);
            scenario.setBankStatus(CanonicalStatus.SUCCESS);
            scenario.setLmsStatus(CanonicalStatus.SUCCESS);

            OffsetDateTime timestamp = OffsetDateTime.of(LocalDate.of(2024, 1, 15), LocalTime.of(10, 0), ZoneOffset.UTC);
            scenario.setOriginatorTimestamp(timestamp);
            scenario.setBankTimestamp(timestamp);
            scenario.setLmsTimestamp(timestamp);
            scenario.setUtrReference("UTR-" + i);
            
            scenarios.add(scenario);
        }

        List<LoanScenario> output = injector.injectAnomalies(scenarios, new Random(42));
        
        Map<AnomalyType, Long> counts = output.stream()
                .filter(s -> s.getAnomalyType() != null)
                .map(LoanScenario::getAnomalyType)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Total anomalies >= 5% of 1000 = 50
        long totalAnomalies = counts.values().stream().mapToLong(Long::longValue).sum();
        assertThat(totalAnomalies).isGreaterThanOrEqualTo(50L);

        // Verify all 10 AnomalyType values are present
        for (AnomalyType type : AnomalyType.values()) {
            assertThat(counts).containsKey(type);
            assertThat(counts.get(type)).isGreaterThan(0L);
        }
    }
}
