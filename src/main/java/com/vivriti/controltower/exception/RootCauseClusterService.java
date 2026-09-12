package com.vivriti.controltower.exception;

import com.vivriti.controltower.canonical.ReconciliationExceptionEntity;
import com.vivriti.controltower.canonical.ReconciliationExceptionRepository;
import com.vivriti.controltower.common.MoneyUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 2 Intelligence: Root-Cause Clustering Service.
 *
 * <p>Clusters open exceptions by {@code PartnerCode + Classification} to identify systemic
 * integration bugs, network drops, fee discrepancies, or schema drift across lending partners.
 * Computes aggregated financial exposure and synthesizes automated remediation recommendations.</p>
 */
@Service
public class RootCauseClusterService {

    public record RootCauseCluster(
            String clusterKey,
            String partnerCode,
            String classification,
            long exceptionCount,
            long totalExposurePaise,
            String totalExposureInr,
            String rootCauseHypothesis,
            String recommendedRemediation,
            String assignedOwnerRole,
            List<String> affectedBusinessEventIds
    ) {}

    private final ReconciliationExceptionRepository exceptionRepository;

    public RootCauseClusterService(ReconciliationExceptionRepository exceptionRepository) {
        this.exceptionRepository = exceptionRepository;
    }

    public List<RootCauseCluster> computeClusters() {
        List<ReconciliationExceptionEntity> openExceptions = exceptionRepository.findByStatus("OPEN");
        if (openExceptions.isEmpty()) {
            openExceptions = exceptionRepository.findAll(); // fallback to all if none explicitly OPEN
        }

        Map<String, List<ReconciliationExceptionEntity>> grouped = openExceptions.stream()
                .collect(Collectors.groupingBy(e -> (e.getPartnerCode() != null ? e.getPartnerCode() : "UNKNOWN") + "::" + e.getClassification()));

        List<RootCauseCluster> clusters = new ArrayList<>();

        for (Map.Entry<String, List<ReconciliationExceptionEntity>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            List<ReconciliationExceptionEntity> list = entry.getValue();

            String[] parts = key.split("::");
            String partnerCode = parts[0];
            String classification = parts.length > 1 ? parts[1] : "UNKNOWN";

            long count = list.size();
            long totalExposurePaise = list.stream().mapToLong(ReconciliationExceptionEntity::getExposureAmountPaise).sum();
            String ownerRole = list.stream().map(ReconciliationExceptionEntity::getOwnerRole).filter(Objects::nonNull).findFirst().orElse("OPERATIONS");

            List<String> eventIds = list.stream().map(ReconciliationExceptionEntity::getBusinessEventId).filter(Objects::nonNull).distinct().limit(20).toList();

            String hypothesis = generateHypothesis(partnerCode, classification, count);
            String remediation = generateRemediation(partnerCode, classification);

            clusters.add(new RootCauseCluster(
                    key,
                    partnerCode,
                    classification,
                    count,
                    totalExposurePaise,
                    MoneyUtils.formatPaiseAsInr(totalExposurePaise),
                    hypothesis,
                    remediation,
                    ownerRole,
                    eventIds
            ));
        }

        // Sort by total exposure descending, then count descending
        clusters.sort((a, b) -> {
            int cmp = Long.compare(b.totalExposurePaise(), a.totalExposurePaise());
            return cmp != 0 ? cmp : Long.compare(b.exceptionCount(), a.exceptionCount());
        });

        return clusters;
    }

    private String generateHypothesis(String partnerCode, String classification, long count) {
        return switch (classification) {
            case "MISSING_FEED_LEG" -> String.format(
                    "Systemic feed transmission latency from %s. %d disbursement instructions lack bank confirmation or LMS booking leg beyond standard cut-off.",
                    partnerCode, count);
            case "AMOUNT_MISMATCH" -> String.format(
                    "Fee deduction drift on %s feed. Instructed gross amount diverges from net LMS booked amount (processing fee / GST withholding anomaly).",
                    partnerCode);
            case "STATUS_MISMATCH" -> String.format(
                    "Asynchronous settlement race condition for %s. Gateway reported failed/pending while LMS recorded successful disbursement.",
                    partnerCode);
            case "DUPLICATE_CALLBACK" -> String.format(
                    "Webhook retry storm from %s payment gateway. Duplicate callbacks delivered for identical UTR / transaction IDs.",
                    partnerCode);
            case "ORPHAN_REVERSAL" -> String.format(
                    "Unmapped chargeback or reversal debit from %s without reference to an existing active disbursement instruction.",
                    partnerCode);
            case "SCHEMA_BREACH" -> String.format(
                    "Partner contract violation in %s schema payload. Missing mandatory headers or negative amounts without reversal tag.",
                    partnerCode);
            case "CONTROL_TOTAL_MISMATCH" -> String.format(
                    "Batch header count/amount checksum drift on %s batch file. File tampering or partial truncation detected.",
                    partnerCode);
            default -> String.format("Unclassified operational variance detected for partner %s across %d records.", partnerCode, count);
        };
    }

    private String generateRemediation(String partnerCode, String classification) {
        return switch (classification) {
            case "MISSING_FEED_LEG" -> "Trigger SFTP/API re-polling for missing bank statements. If past 24h grace window, initiate partner enquiry.";
            case "AMOUNT_MISMATCH" -> "Verify partner fee schedule configuration against originator master agreement; recalculate net disbursement ledger.";
            case "STATUS_MISMATCH" -> "Halt interest accrual on disputed loans. Query bank gateway API for final UTR settlement state.";
            case "DUPLICATE_CALLBACK" -> "Verify deduplication filter and idempotency key constraints; discard uncommitted duplicate records.";
            case "ORPHAN_REVERSAL" -> "Route to Finance Ops for manual bank statement matching and reversal ledger reconciliation.";
            case "SCHEMA_BREACH" -> "Reject bad payload with HTTP 422; notify partner integration engineering to fix contract compliance.";
            case "CONTROL_TOTAL_MISMATCH" -> "HOLD batch immediately. Request clean regenerate from partner and verify file transmission checksum.";
            default -> "Assign to Co-lending Operations for standard break resolution workflow.";
        };
    }
}

