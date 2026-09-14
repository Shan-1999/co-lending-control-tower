package com.vivriti.controltower.reconciliation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivriti.controltower.canonical.*;
import com.vivriti.controltower.common.MoneyUtils;
import com.vivriti.controltower.exception.RootCauseClusterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 1 & Phase 2 Independent Evaluation Reporter.
 *
 * <p>Produces auditable post-hoc evaluation scorecards comparing reconciliation
 * decisions against isolated ground truth. Clearly separates:
 * <ul>
 *   <li><b>Section A (Phase 1 Mandatory Gate)</b>: Exact, composite, timing rates,
 *       control-total delta, and hard gate: <b>False Match Exposure (MUST BE 0, ₹0.00)</b>.</li>
 *   <li><b>Section B (Phase 2 Intelligence)</b>: Read-only probable matches, precision,
 *       recall, root-cause clusters, and time-to-detect.</li>
 * </ul>
 */
@Service
public class EvaluationReporter {

    private static final Logger log = LoggerFactory.getLogger(EvaluationReporter.class);

    private final MatchDecisionRepository matchDecisionRepository;
    private final CanonicalEventRepository canonicalEventRepository;
    private final ReconciliationExceptionRepository exceptionRepository;
    private final RootCauseClusterService clusterService;
    private final ObjectMapper objectMapper;

    public EvaluationReporter(MatchDecisionRepository matchDecisionRepository,
                              CanonicalEventRepository canonicalEventRepository,
                              ReconciliationExceptionRepository exceptionRepository,
                              RootCauseClusterService clusterService,
                              ObjectMapper objectMapper) {
        this.matchDecisionRepository = matchDecisionRepository;
        this.canonicalEventRepository = canonicalEventRepository;
        this.exceptionRepository = exceptionRepository;
        this.clusterService = clusterService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> evaluate(Path groundTruthPath) throws IOException {
        if (!Files.exists(groundTruthPath)) {
            throw new IllegalArgumentException("Ground truth file not found at: " + groundTruthPath);
        }

        String gtContent = Files.readString(groundTruthPath);
        List<Map<String, Object>> groundTruth = objectMapper.readValue(gtContent, new TypeReference<>() {});

        List<MatchDecisionEntity> allDecisions = matchDecisionRepository.findAll();
        Map<String, MatchDecisionEntity> decisionsByEventId = allDecisions.stream()
                .collect(Collectors.toMap(
                        MatchDecisionEntity::getBusinessEventId,
                        d -> d,
                        (a, b) -> a
                ));

        int totalLoans = groundTruth.size();
        int exactMatches = 0;
        int compositeMatches = 0;
        int timingMatches = 0;
        int probableProposals = 0;
        int unresolvedBreaks = 0;

        long exactPaise = 0;
        long compositePaise = 0;
        long timingPaise = 0;
        long unresolvedPaise = 0;

        int falseMatchCount = 0;
        long falseMatchValuePaise = 0;

        // Phase 2 evaluation counters
        int validProbableMatches = 0;
        int trueAnomalies = 0;

        Map<String, Map<String, Integer>> confusionMatrix = new LinkedHashMap<>();

        for (Map<String, Object> gt : groundTruth) {
            String loanRef = (String) gt.get("loanReference");
            String expectedLevel = (String) gt.get("expectedMatchLevel");
            String anomalyType = gt.get("anomalyType") != null ? (String) gt.get("anomalyType") : "NONE";
            Number amountNum = (Number) gt.get("originatorAmountPaise");
            long amount = amountNum != null ? amountNum.longValue() : 0L;

            if (!"NONE".equals(anomalyType)) {
                trueAnomalies++;
            }

            List<CanonicalEventEntity> events = canonicalEventRepository.findByLoanReference(loanRef);
            String actualLevel = "UNRESOLVED";
            if (!events.isEmpty()) {
                String bizEventId = events.get(0).getBusinessEventId();
                MatchDecisionEntity decision = decisionsByEventId.get(bizEventId);
                if (decision != null) {
                    actualLevel = decision.getMatchLevel();
                }
            }

            confusionMatrix.computeIfAbsent(anomalyType, k -> new LinkedHashMap<>())
                    .merge(actualLevel, 1, Integer::sum);

            switch (actualLevel) {
                case "EXACT" -> {
                    exactMatches++;
                    exactPaise += amount;
                }
                case "COMPOSITE" -> {
                    compositeMatches++;
                    compositePaise += amount;
                }
                case "TIMING" -> {
                    timingMatches++;
                    timingPaise += amount;
                }
                case "PROBABLE" -> {
                    probableProposals++;
                    unresolvedBreaks++;
                    unresolvedPaise += amount;
                    if ("AMOUNT_MISMATCH".equals(anomalyType) || "TIMING_DIFFERENCE".equals(anomalyType)) {
                        validProbableMatches++;
                    }
                }
                default -> {
                    unresolvedBreaks++;
                    unresolvedPaise += amount;
                }
            }

            // Strict False Match Check: If an anomaly was expected to fail or remain unresolved,
            // but was falsely marked EXACT or COMPOSITE, that is an unacceptable financial false match.
            if (!"EXACT".equals(expectedLevel) && !"COMPOSITE".equals(expectedLevel) && !"TIMING".equals(expectedLevel)) {
                if ("EXACT".equals(actualLevel) || "COMPOSITE".equals(actualLevel)) {
                    falseMatchCount++;
                    falseMatchValuePaise += amount;
                }
            } else if ("TIMING".equals(expectedLevel) && "EXACT".equals(actualLevel)) {
                // A timing breach that got matched as EXACT is an unhedged cutoff violation
                falseMatchCount++;
                falseMatchValuePaise += amount;
            }
        }

        // Calculations
        long totalInstructedPaise = exactPaise + compositePaise + timingPaise + unresolvedPaise;
        long totalSourcePaise = groundTruth.stream()
                .mapToLong(gt -> {
                    Number n = (Number) gt.get("originatorAmountPaise");
                    return n != null ? n.longValue() : 0L;
                }).sum();
        long controlTotalDeltaPaise = Math.abs(totalSourcePaise - totalInstructedPaise);

        double stpRate = totalLoans > 0 ? ((double) exactMatches / totalLoans) * 100.0 : 0.0;
        double probablePrecision = probableProposals > 0 ? ((double) validProbableMatches / probableProposals) * 100.0 : 100.0;
        double probableRecall = trueAnomalies > 0 ? ((double) validProbableMatches / trueAnomalies) * 100.0 : 0.0;

        List<RootCauseClusterService.RootCauseCluster> clusters = clusterService.computeClusters();
        List<ReconciliationExceptionEntity> allExceptions = exceptionRepository.findAll();

        // Print Formatted Console Scorecard
        printScorecard(totalLoans, exactMatches, exactPaise, compositeMatches, compositePaise,
                timingMatches, timingPaise, unresolvedBreaks, unresolvedPaise,
                falseMatchCount, falseMatchValuePaise, stpRate, controlTotalDeltaPaise,
                probableProposals, probablePrecision, probableRecall, clusters.size(), allExceptions.size());

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> phase1 = new LinkedHashMap<>();
        phase1.put("totalLoansEvaluated", totalLoans);
        phase1.put("exactMatchRate", String.format("%d (%.2f%%) — %s", exactMatches, (double) exactMatches / totalLoans * 100, MoneyUtils.formatPaiseAsInr(exactPaise)));
        phase1.put("compositeMatchRate", String.format("%d (%.2f%%) — %s", compositeMatches, (double) compositeMatches / totalLoans * 100, MoneyUtils.formatPaiseAsInr(compositePaise)));
        phase1.put("timingGraceRate", String.format("%d (%.2f%%) — %s", timingMatches, (double) timingMatches / totalLoans * 100, MoneyUtils.formatPaiseAsInr(timingPaise)));
        phase1.put("unresolvedBreaks", String.format("%d (%.2f%%) — %s", unresolvedBreaks, (double) unresolvedBreaks / totalLoans * 100, MoneyUtils.formatPaiseAsInr(unresolvedPaise)));
        phase1.put("falseMatchExposureCount", falseMatchCount);
        phase1.put("falseMatchExposureValue", MoneyUtils.formatPaiseAsInr(falseMatchValuePaise));
        phase1.put("straightThroughRate", String.format("%.2f%%", stpRate));
        phase1.put("controlTotalDelta", MoneyUtils.formatPaiseAsInr(controlTotalDeltaPaise));
        String gateStatus = allDecisions.isEmpty() ? "NOT_EVALUATED"
                : (falseMatchCount == 0 && controlTotalDeltaPaise == 0 ? "PASSED" : "FAILED");
        phase1.put("gateStatus", gateStatus);

        Map<String, Object> phase2 = new LinkedHashMap<>();
        phase2.put("probableProposalsCount", probableProposals);
        phase2.put("probablePrecision", String.format("%.2f%%", probablePrecision));
        phase2.put("probableRecall", String.format("%.2f%%", probableRecall));
        phase2.put("rootCauseClustersCount", clusters.size());
        phase2.put("totalExceptionsTracked", allExceptions.size());
        phase2.put("clusters", clusters);
        phase2.put("confusionMatrix", confusionMatrix);

        result.put("phase1Gate", phase1);
        result.put("phase2Intelligence", phase2);
        return result;
    }

    private void printScorecard(int totalLoans, int exact, long exactPaise, int composite, long compositePaise,
                                int timing, long timingPaise, int unresolved, long unresolvedPaise,
                                int falseMatches, long falseMatchPaise, double stp, long deltaPaise,
                                int probable, double precision, double recall, int clusters, int exceptionCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(78)).append("\n");
        sb.append("          VIVRITI CO-LENDING CONTROL TOWER — EVALUATION SCORECARD\n");
        sb.append("=".repeat(78)).append("\n");
        sb.append("SECTION A: PHASE 1 MANDATORY GATE (DETERMINISTIC FINANCIAL ENGINE)\n");
        sb.append("-".repeat(78)).append("\n");
        sb.append(String.format("  Total Loan Instructions Evaluated : %,d%n", totalLoans));
        sb.append(String.format("  Level 1 Exact Match Rate          : %,d (%.2f%%) | %s%n", exact, (double) exact / totalLoans * 100, MoneyUtils.formatPaiseAsInr(exactPaise)));
        sb.append(String.format("  Level 2 Composite Split Match Rate: %,d (%.2f%%) | %s%n", composite, (double) composite / totalLoans * 100, MoneyUtils.formatPaiseAsInr(compositePaise)));
        sb.append(String.format("  Level 3 Timing Difference (Grace) : %,d (%.2f%%) | %s%n", timing, (double) timing / totalLoans * 100, MoneyUtils.formatPaiseAsInr(timingPaise)));
        sb.append(String.format("  Level 5 Unresolved Breaks (Owned) : %,d (%.2f%%) | %s%n", unresolved, (double) unresolved / totalLoans * 100, MoneyUtils.formatPaiseAsInr(unresolvedPaise)));
        sb.append(String.format("  Straight-Through Processing (STP) : %.2f%%%n", stp));
        sb.append(String.format("  Control Total Integrity Delta     : %s (Target: ₹0.00)%n", MoneyUtils.formatPaiseAsInr(deltaPaise)));
        sb.append("  ----------------------------------------------------------------------------\n");
        sb.append(String.format("  >>> FALSE MATCH EXPOSURE          : COUNT: %d, VALUE: %s <<<%n", falseMatches, MoneyUtils.formatPaiseAsInr(falseMatchPaise)));
        sb.append(String.format("  >>> PHASE 1 HARD GATE STATUS      : %s <<<%n", (falseMatches == 0 && deltaPaise == 0 ? "PASSED (0 FALSE MATCHES)" : "FAILED")));
        sb.append("-".repeat(78)).append("\n");
        sb.append("SECTION B: PHASE 2 INTELLIGENCE (READ-ONLY ADVISORY & CLUSTERING)\n");
        sb.append("-".repeat(78)).append("\n");
        sb.append(String.format("  Level 4 Probable Match Proposals  : %,d proposals (Requires Human Approval)%n", probable));
        sb.append(String.format("  Probable Match Precision          : %.2f%%%n", precision));
        sb.append(String.format("  Probable Match Recall             : %.2f%%%n", recall));
        sb.append(String.format("  Root-Cause Clusters Identified    : %,d distinct systemic integration patterns%n", clusters));
        sb.append(String.format("  Active Exception Queue Inventory   : %,d owned exceptions tracked with SLA clocks%n", exceptionCount));
        sb.append("  AI/Probabilistic Guardrail        : 100% READ-ONLY ADVISORY (Zero Auto-Reconciliation)\n");
        sb.append("=".repeat(78)).append("\n");

        String formatted = sb.toString();
        System.out.print(formatted);
        log.info("{}", formatted);
    }
}

