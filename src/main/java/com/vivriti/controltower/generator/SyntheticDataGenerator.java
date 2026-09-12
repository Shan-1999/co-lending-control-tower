package com.vivriti.controltower.generator;

import com.vivriti.controltower.common.IdGenerator;
import com.vivriti.controltower.common.enums.CanonicalStatus;
import com.vivriti.controltower.common.enums.EventType;
import com.vivriti.controltower.common.enums.SourceSystem;
import com.vivriti.controltower.generator.groundtruth.GroundTruthRecord;
import com.vivriti.controltower.generator.groundtruth.GroundTruthWriter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class SyntheticDataGenerator {

    private final AnomalyInjector anomalyInjector;
    private final PartnerAlphaWriter alphaWriter = new PartnerAlphaWriter();
    private final PartnerBetaWriter betaWriter = new PartnerBetaWriter();
    private final PartnerGammaWriter gammaWriter = new PartnerGammaWriter();
    private final GroundTruthWriter groundTruthWriter = new GroundTruthWriter();
    private final DataQualityReportWriter reportWriter = new DataQualityReportWriter();

    public SyntheticDataGenerator(AnomalyInjector anomalyInjector) {
        this.anomalyInjector = anomalyInjector;
    }

    public void generate(long seed, int loanCount, java.nio.file.Path outputDir) throws Exception {
        generate(seed, loanCount, outputDir.toString());
    }

    public void generate(long seed, int loanCount, String outputDir) throws Exception {
        Random random = new Random(seed);
        List<LoanScenario> scenarios = new ArrayList<>(loanCount);

        LocalDate[] dates = {
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 16),
                LocalDate.of(2024, 1, 17)
        };

        String[] partners = {"PARTNER_ALPHA", "PARTNER_BETA", "PARTNER_GAMMA"};

        for (int i = 1; i <= loanCount; i++) {
            LoanScenario scenario = new LoanScenario();
            scenario.setLoanReference(String.format("LOAN-%06d", i));
            scenario.setPartnerCode(partners[random.nextInt(partners.length)]);
            
            int dayIndex = random.nextInt(3);
            scenario.setBusinessDay(dayIndex + 1);
            
            long amount = 100000L + random.nextInt(900000); // 1000 to 10000 INR
            scenario.setOriginatorAmount(amount);
            scenario.setBankAmount(amount);
            scenario.setLmsAmount(amount);
            
            scenario.setOriginatorStatus(CanonicalStatus.SUCCESS);
            scenario.setBankStatus(CanonicalStatus.SUCCESS);
            scenario.setLmsStatus(CanonicalStatus.SUCCESS);

            LocalDate date = dates[dayIndex];
            LocalTime time = LocalTime.of(9 + random.nextInt(8), random.nextInt(60));
            OffsetDateTime timestamp = OffsetDateTime.of(date, time, ZoneOffset.UTC);
            
            scenario.setOriginatorTimestamp(timestamp);
            scenario.setBankTimestamp(timestamp.plusMinutes(random.nextInt(60)));
            scenario.setLmsTimestamp(timestamp.plusMinutes(random.nextInt(120)));
            
            scenario.setUtrReference("UTR-" + scenario.getLoanReference() + "-" + random.nextInt(1000));
            
            scenarios.add(scenario);
        }

        List<LoanScenario> finalScenarios = anomalyInjector.injectAnomalies(scenarios, random);

        File outDir = new File(outputDir);
        if (!outDir.exists()) outDir.mkdirs();

        int totalRecords = 0;
        Map<String, Long> recordsBySourceSystem = new HashMap<>();
        recordsBySourceSystem.put("ORIGINATOR", 0L);
        recordsBySourceSystem.put("BANK", 0L);
        recordsBySourceSystem.put("LMS", 0L);

        for (int day = 1; day <= 3; day++) {
            final int currentDay = day;
            for (String partner : partners) {
                List<LoanScenario> partnerScenarios = finalScenarios.stream()
                        .filter(s -> s.getBusinessDay() == currentDay && s.getPartnerCode().equals(partner))
                        .toList();

                for (SourceSystem source : SourceSystem.values()) {
                    List<FeedRecord> records = new ArrayList<>();
                    
                    String batchId = IdGenerator.newBatchId(partner, source.name(), dates[currentDay - 1]);

                    for (LoanScenario s : partnerScenarios) {
                        if (source == SourceSystem.ORIGINATOR) {
                            if (s.getAnomalyType() != AnomalyType.ORPHAN_REVERSAL) {
                                records.add(new FeedRecord(
                                        s.getLoanReference(), s.getOriginatorAmount(), s.getOriginatorStatus(),
                                        s.getOriginatorTimestamp(), s.getUtrReference(), s.getPartnerCode(),
                                        source, EventType.DISBURSEMENT_INSTRUCTION, null,
                                        "CORR-" + s.getLoanReference(), "EVT-" + s.getLoanReference()
                                ));
                            }
                        } else if (source == SourceSystem.BANK) {
                            if (s.getAnomalyType() == AnomalyType.ORPHAN_REVERSAL && s.getReversalReference() != null) {
                                records.add(new FeedRecord(
                                        s.getLoanReference(), 10000L, CanonicalStatus.SUCCESS,
                                        s.getOriginatorTimestamp().plusHours(1), s.getUtrReference(), s.getPartnerCode(),
                                        source, EventType.REVERSAL, s.getReversalReference(),
                                        "CORR-REV-" + s.getLoanReference(), "EVT-REV-" + s.getLoanReference()
                                ));
                            } else if (s.getBankAmount() != null) {
                                if (s.getSplitAmounts() != null && !s.getSplitAmounts().isEmpty()) {
                                    for (int splitIdx = 0; splitIdx < s.getSplitAmounts().size(); splitIdx++) {
                                        records.add(new FeedRecord(
                                                s.getLoanReference(), s.getSplitAmounts().get(splitIdx), s.getBankStatus(),
                                                s.getBankTimestamp().plusMinutes(splitIdx * 5L), s.getUtrReference() + "-" + splitIdx, s.getPartnerCode(),
                                                source, EventType.SETTLEMENT_DEBIT, null,
                                                "CORR-" + s.getLoanReference(), "EVT-" + s.getLoanReference() + "-S" + splitIdx
                                        ));
                                    }
                                } else {
                                    records.add(new FeedRecord(
                                            s.getLoanReference(), s.getBankAmount(), s.getBankStatus(),
                                            s.getBankTimestamp(), s.getUtrReference(), s.getPartnerCode(),
                                            source, EventType.SETTLEMENT_DEBIT, null,
                                            "CORR-" + s.getLoanReference(), "EVT-" + s.getLoanReference()
                                    ));
                                    
                                    if (s.isDuplicateBankEvent()) {
                                        records.add(new FeedRecord(
                                                s.getLoanReference(), s.getBankAmount(), s.getBankStatus(),
                                                s.getBankTimestamp(), s.getUtrReference(), s.getPartnerCode(),
                                                source, EventType.SETTLEMENT_DEBIT, null,
                                                "CORR-" + s.getLoanReference(), "EVT-" + s.getLoanReference() + "-DUP"
                                        ));
                                    }
                                }
                            }
                        } else if (source == SourceSystem.LMS) {
                            if (s.getLmsAmount() != null) {
                                records.add(new FeedRecord(
                                        s.getLoanReference(), s.getLmsAmount(), s.getLmsStatus(),
                                        s.getLmsTimestamp(), s.getUtrReference(), s.getPartnerCode(),
                                        source, EventType.LMS_BOOKING, null,
                                        "CORR-" + s.getLoanReference(), "EVT-" + s.getLoanReference()
                                ));
                            }
                        }
                    }

                    if (records.isEmpty()) continue;
                    
                    totalRecords += records.size();
                    recordsBySourceSystem.put(source.name(), recordsBySourceSystem.get(source.name()) + records.size());

                    boolean schemaBreach = partnerScenarios.stream().anyMatch(LoanScenario::isSchemaBreach);
                    boolean batchTotalMismatch = partnerScenarios.stream().anyMatch(LoanScenario::isBatchTotalMismatch);

                    String fileName = String.format("%s_%s_day%d", partner.toLowerCase(), source.name().toLowerCase(), currentDay);
                    File file;
                    if (partner.equals("PARTNER_ALPHA")) {
                        file = new File(outDir, fileName + ".json");
                        alphaWriter.writeFeed(file, batchId, dates[currentDay - 1], records, schemaBreach, batchTotalMismatch);
                    } else if (partner.equals("PARTNER_BETA")) {
                        file = new File(outDir, fileName + ".csv");
                        betaWriter.writeFeed(file, batchId, dates[currentDay - 1], records, schemaBreach, batchTotalMismatch);
                    } else {
                        file = new File(outDir, fileName + ".txt");
                        gammaWriter.writeFeed(file, batchId, dates[currentDay - 1], records, schemaBreach, batchTotalMismatch);
                    }
                }
            }
        }

        List<GroundTruthRecord> gtRecords = finalScenarios.stream().map(s -> {
            String matchLevel = "EXACT";
            String exceptionClass = null;
            if (s.getAnomalyType() != null) {
                switch (s.getAnomalyType()) {
                    case TIMING_DIFFERENCE: matchLevel = "TIMING"; break;
                    case COMPOSITE_SPLIT: matchLevel = "COMPOSITE"; break;
                    case BATCH_TOTAL_MISMATCH: matchLevel = "EXACT"; break;
                    default: matchLevel = "UNRESOLVED"; exceptionClass = s.getAnomalyType().name(); break;
                }
            }
            return new GroundTruthRecord(
                    s.getLoanReference(),
                    matchLevel,
                    s.getAnomalyType() != null ? s.getAnomalyType().name() : null,
                    exceptionClass,
                    s.getOriginatorAmount(),
                    s.getBankAmount(),
                    s.getLmsAmount(),
                    s.getBankAmount() != null && s.getLmsAmount() != null
            );
        }).toList();

        File gtDir = new File(outDir, "groundtruth");
        groundTruthWriter.writeGroundTruth(gtDir, gtRecords);
        reportWriter.writeReport(outDir, finalScenarios, totalRecords, recordsBySourceSystem);
        
        System.out.println("Data generation complete.");
        System.out.println("Total loans: " + finalScenarios.size());
        System.out.println("Total records: " + totalRecords);
    }
}
