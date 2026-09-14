package com.vivriti.controltower.generator;

import com.vivriti.controltower.common.enums.CanonicalStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class AnomalyInjector {

    @Value("${controltower.generator.anomaly.missing-bank-leg-rate:0.01}")
    private double missingBankLegRate;

    @Value("${controltower.generator.anomaly.missing-lms-leg-rate:0.01}")
    private double missingLmsLegRate;

    @Value("${controltower.generator.anomaly.duplicate-bank-event-rate:0.008}")
    private double duplicateBankEventRate;

    @Value("${controltower.generator.anomaly.amount-mismatch-rate:0.02}")
    private double amountMismatchRate;

    @Value("${controltower.generator.anomaly.status-mismatch-rate:0.008}")
    private double statusMismatchRate;

    @Value("${controltower.generator.anomaly.timing-difference-rate:0.015}")
    private double timingDifferenceRate;

    @Value("${controltower.generator.anomaly.composite-split-rate:0.008}")
    private double compositeSplitRate;

    @Value("${controltower.generator.anomaly.orphan-reversal-rate:0.005}")
    private double orphanReversalRate;

    @Value("${controltower.generator.anomaly.schema-breach-rate:0.005}")
    private double schemaBreachRate;

    @Value("${controltower.generator.anomaly.batch-total-mismatch-rate:0.005}")
    private double batchTotalMismatchRate;

    public List<LoanScenario> injectAnomalies(List<LoanScenario> scenarios, Random random) {
        int totalLoans = scenarios.size();
        
        List<AnomalyType> assignedAnomalies = new ArrayList<>();
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * missingBankLegRate)), AnomalyType.MISSING_BANK_LEG));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * missingLmsLegRate)), AnomalyType.MISSING_LMS_LEG));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * duplicateBankEventRate)), AnomalyType.DUPLICATE_BANK_EVENT));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * amountMismatchRate)), AnomalyType.AMOUNT_MISMATCH));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * statusMismatchRate)), AnomalyType.STATUS_MISMATCH));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * timingDifferenceRate)), AnomalyType.TIMING_DIFFERENCE));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * compositeSplitRate)), AnomalyType.COMPOSITE_SPLIT));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * orphanReversalRate)), AnomalyType.ORPHAN_REVERSAL));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * schemaBreachRate)), AnomalyType.SCHEMA_BREACH));
        assignedAnomalies.addAll(Collections.nCopies(Math.max(1, (int)(totalLoans * batchTotalMismatchRate)), AnomalyType.BATCH_TOTAL_MISMATCH));

        int minAnomalies = Math.max((int)(totalLoans * 0.05), 10);
        while (assignedAnomalies.size() < minAnomalies && assignedAnomalies.size() < totalLoans) {
            AnomalyType[] types = AnomalyType.values();
            assignedAnomalies.add(types[random.nextInt(types.length)]);
        }

        while (assignedAnomalies.size() < totalLoans) {
            assignedAnomalies.add(null);
        }

        Collections.shuffle(assignedAnomalies, random);

        // Ensure Partner Beta Day 1 remains clean for balanced batch close demonstration
        // by transferring any anomalies assigned to Partner Beta Day 1 to other scenarios
        for (int i = 0; i < totalLoans; i++) {
            LoanScenario s = scenarios.get(i);
            if (s.getBusinessDay() == 1 && "PARTNER_BETA".equals(s.getPartnerCode())) {
                AnomalyType t = assignedAnomalies.get(i);
                if (t != null) {
                    for (int j = 0; j < totalLoans; j++) {
                        LoanScenario target = scenarios.get(j);
                        if (!(target.getBusinessDay() == 1 && "PARTNER_BETA".equals(target.getPartnerCode()))
                                && assignedAnomalies.get(j) == null) {
                            assignedAnomalies.set(j, t);
                            assignedAnomalies.set(i, null);
                            break;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < totalLoans; i++) {
            LoanScenario scenario = scenarios.get(i);
            AnomalyType type = assignedAnomalies.get(i);
            scenario.setAnomalyType(type);

            if (type == null) continue;

            switch (type) {
                case MISSING_BANK_LEG:
                    scenario.setBankAmount(null);
                    scenario.setBankStatus(null);
                    scenario.setBankTimestamp(null);
                    break;
                case MISSING_LMS_LEG:
                    scenario.setLmsAmount(null);
                    scenario.setLmsStatus(null);
                    scenario.setLmsTimestamp(null);
                    break;
                case DUPLICATE_BANK_EVENT:
                    scenario.setDuplicateBankEvent(true);
                    break;
                case AMOUNT_MISMATCH:
                    scenario.setLmsAmount(scenario.getOriginatorAmount() - 10000L); // 100 INR processing fee
                    break;
                case STATUS_MISMATCH:
                    scenario.setBankStatus(CanonicalStatus.FAILED);
                    break;
                case TIMING_DIFFERENCE:
                    java.time.LocalDate bDate = scenario.getOriginatorTimestamp().toLocalDate();
                    scenario.setBankTimestamp(bDate.atTime(18, 30).atOffset(java.time.ZoneOffset.UTC));
                    break;
                case COMPOSITE_SPLIT:
                    long total = scenario.getOriginatorAmount();
                    long split1 = total / 2;
                    long split2 = total - split1;
                    scenario.setSplitAmounts(List.of(split1, split2));
                    break;
                case ORPHAN_REVERSAL:
                    scenario.setReversalReference("ORPHAN-REV-" + scenario.getLoanReference());
                    scenario.setOriginatorAmount(0L);
                    scenario.setLmsAmount(null);
                    scenario.setLmsStatus(null);
                    break;
                case SCHEMA_BREACH:
                    scenario.setSchemaBreach(true);
                    scenario.setOriginatorAmount(-scenario.getOriginatorAmount());
                    break;
                case BATCH_TOTAL_MISMATCH:
                    scenario.setBatchTotalMismatch(true);
                    break;
            }
        }

        return scenarios;
    }
}
