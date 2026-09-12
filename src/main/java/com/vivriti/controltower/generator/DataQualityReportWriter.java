package com.vivriti.controltower.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataQualityReportWriter {

    public void writeReport(File outputDir, List<LoanScenario> scenarios, int totalRecords, Map<String, Long> recordsBySourceSystem) throws IOException {
        File file = new File(outputDir, "data_quality_report.md");

        Map<AnomalyType, Long> anomalyCounts = scenarios.stream()
                .filter(s -> s.getAnomalyType() != null)
                .collect(Collectors.groupingBy(LoanScenario::getAnomalyType, Collectors.counting()));

        long totalAnomalies = scenarios.stream().filter(s -> s.getAnomalyType() != null).count();
        double anomalyRate = (double) totalAnomalies / scenarios.size() * 100;

        Map<Integer, Long> byDay = scenarios.stream()
                .collect(Collectors.groupingBy(LoanScenario::getBusinessDay, Collectors.counting()));

        Map<String, Long> byPartner = scenarios.stream()
                .collect(Collectors.groupingBy(LoanScenario::getPartnerCode, Collectors.counting()));

        long totalOriginatorPaise = scenarios.stream().mapToLong(LoanScenario::getOriginatorAmount).sum();
        long totalBankPaise = scenarios.stream().filter(s -> s.getBankAmount() != null).mapToLong(LoanScenario::getBankAmount).sum();
        long totalLmsPaise = scenarios.stream().filter(s -> s.getLmsAmount() != null).mapToLong(LoanScenario::getLmsAmount).sum();

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("# Synthetic Data Quality Report");
            writer.println();
            writer.println("## Overview");
            writer.printf("- **Total Loans:** %d%n", scenarios.size());
            writer.printf("- **Total Records Generated:** %d%n", totalRecords);
            writer.printf("- **Overall Anomaly Rate:** %.2f%%%n", anomalyRate);
            writer.println();
            
            writer.println("## Records by Source System");
            recordsBySourceSystem.forEach((sys, count) -> writer.printf("- **%s:** %d%n", sys, count));
            writer.println();

            writer.println("## Anomalies by Type");
            for (AnomalyType type : AnomalyType.values()) {
                writer.printf("- **%s:** %d%n", type.name(), anomalyCounts.getOrDefault(type, 0L));
            }
            writer.println();

            writer.println("## Distribution by Business Day");
            byDay.forEach((day, count) -> writer.printf("- **Day %d:** %d loans%n", day, count));
            writer.println();

            writer.println("## Distribution by Partner");
            byPartner.forEach((partner, count) -> writer.printf("- **%s:** %d loans%n", partner, count));
            writer.println();

            writer.println("## Total Paise by Source System");
            writer.printf("- **ORIGINATOR:** %d%n", totalOriginatorPaise);
            writer.printf("- **BANK:** %d%n", totalBankPaise);
            writer.printf("- **LMS:** %d%n", totalLmsPaise);
        }
    }
}
