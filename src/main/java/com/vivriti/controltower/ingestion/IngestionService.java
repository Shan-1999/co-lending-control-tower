package com.vivriti.controltower.ingestion;

import com.vivriti.controltower.canonical.*;
import com.vivriti.controltower.common.HashUtils;
import com.vivriti.controltower.common.IdGenerator;
import com.vivriti.controltower.common.enums.BatchStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class IngestionService {

    private final List<PartnerFeedAdapter> adapters;
    private final SourceBatchRepository batchRepository;
    private final RawSourceRecordRepository rawRepository;
    private final CanonicalEventRepository canonicalRepository;
    private final QuarantineService quarantineService;
    private final BatchValidator batchValidator;

    public IngestionService(List<PartnerFeedAdapter> adapters,
                            SourceBatchRepository batchRepository,
                            RawSourceRecordRepository rawRepository,
                            CanonicalEventRepository canonicalRepository,
                            QuarantineService quarantineService,
                            BatchValidator batchValidator) {
        this.adapters = adapters;
        this.batchRepository = batchRepository;
        this.rawRepository = rawRepository;
        this.canonicalRepository = canonicalRepository;
        this.quarantineService = quarantineService;
        this.batchValidator = batchValidator;
    }

    public IngestionResult ingestDirectory(Path dataDir) {
        int total = 0, processed = 0, quarantined = 0, skipped = 0;
        try (Stream<Path> paths = Files.walk(dataDir)) {
            List<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".json") || name.endsWith(".csv") || name.endsWith(".txt");
                    })
                    // Skip ground truth files
                    .filter(p -> !p.toString().contains("ground_truth"))
                    .filter(p -> !p.toString().contains("data_quality"))
                    .toList();

            for (Path file : files) {
                String fileName = file.getFileName().toString().toUpperCase();
                String partnerCode = detectPartnerCode(fileName);
                String sourceSystem = detectSourceSystem(fileName);

                PartnerFeedAdapter adapter = findAdapter(partnerCode);
                if (adapter == null) continue;

                try {
                    ParsedBatch batch = adapter.parse(partnerCode, sourceSystem, file);
                    if (batch == null || batch.records() == null) continue;
                    
                    // Check if batch already exists (idempotency)
                    if (batchRepository.existsById(batch.batchId())) {
                        skipped += batch.records().size();
                        total += batch.records().size();
                        continue;
                    }

                    IngestionResult res = ingestBatch(batch);
                    total += res.totalRecords();
                    processed += res.processedRecords();
                    quarantined += res.quarantinedRecords();
                    skipped += res.skippedDuplicates();
                } catch (Exception e) {
                    // Log error but continue with other files
                    System.err.println("Error processing file: " + file + " — " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error ingesting directory: " + dataDir, e);
        }
        return new IngestionResult(total, processed, quarantined, skipped, "COMPLETED");
    }

    /**
     * Ingest a single file by path, partner code, and source system.
     */
    public IngestionResult ingestFile(Path filePath, String partnerCode, String sourceSystem) {
        PartnerFeedAdapter adapter = findAdapter(partnerCode);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter found for partner: " + partnerCode);
        }
        ParsedBatch batch = adapter.parse(partnerCode, sourceSystem, filePath);
        return ingestBatch(batch);
    }

    private String detectPartnerCode(String fileName) {
        if (fileName.contains("ALPHA")) return "PARTNER_ALPHA";
        if (fileName.contains("BETA")) return "PARTNER_BETA";
        if (fileName.contains("GAMMA")) return "PARTNER_GAMMA";
        // Default based on extension
        if (fileName.endsWith(".JSON")) return "PARTNER_ALPHA";
        if (fileName.endsWith(".CSV")) return "PARTNER_BETA";
        if (fileName.endsWith(".TXT")) return "PARTNER_GAMMA";
        return "PARTNER_ALPHA";
    }

    private String detectSourceSystem(String fileName) {
        if (fileName.contains("ORIGINATOR")) return "ORIGINATOR";
        if (fileName.contains("BANK")) return "BANK";
        if (fileName.contains("LMS")) return "LMS";
        if (fileName.contains("SETTLEMENT")) return "BANK";
        return "ORIGINATOR"; // default
    }

    private PartnerFeedAdapter findAdapter(String partnerCode) {
        return adapters.stream()
                .filter(a -> a.supports(partnerCode))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public IngestionResult ingestBatch(ParsedBatch parsedBatch) {
        SourceBatchEntity batchEntity = new SourceBatchEntity();
        batchEntity.setBatchId(parsedBatch.batchId());
        batchEntity.setPartnerCode(parsedBatch.partnerCode());
        batchEntity.setSourceSystem(parsedBatch.sourceSystem());
        batchEntity.setBusinessDate(parsedBatch.businessDate());
        batchEntity.setDeclaredCount(parsedBatch.declaredCount());
        batchEntity.setDeclaredAmountPaise(parsedBatch.declaredAmountPaise());
        batchEntity.setStatus(BatchStatus.RECEIVED.name());
        batchEntity.setReceivedAt(OffsetDateTime.now());
        batchRepository.save(batchEntity);

        int processed = 0;
        int quarantined = 0;
        int skipped = 0;
        int total = parsedBatch.records().size();

        for (int i = 0; i < total; i++) {
            ParsedFeedRecord rec = parsedBatch.records().get(i);
            String payloadHash = HashUtils.sha256(rec.rawPayload());
            
            if (rawRepository.existsByBatchIdAndPayloadHash(batchEntity.getBatchId(), payloadHash)) {
                skipped++;
                continue;
            }

            RawSourceRecordEntity raw = new RawSourceRecordEntity();
            raw.setRecordId(IdGenerator.newRecordId(batchEntity.getBatchId(), i));
            raw.setBatch(batchEntity);
            raw.setSourceSystem(parsedBatch.sourceSystem());
            raw.setPayloadHash(payloadHash);
            raw.setRawPayload(rec.rawPayload());
            raw.setReceivedAt(OffsetDateTime.now());

            boolean isValid = quarantineService.validateAndQuarantine(rec, raw);
            rawRepository.save(raw);

            if (isValid) {
                CanonicalEventEntity canonical = new CanonicalEventEntity();
                canonical.setEventId(IdGenerator.newId());
                canonical.setRawRecord(raw);
                canonical.setBusinessEventId(rec.businessEventId());
                canonical.setCorrelationId(rec.correlationId());
                canonical.setLoanReference(rec.loanReference());
                canonical.setPartnerCode(parsedBatch.partnerCode());
                canonical.setSourceSystem(parsedBatch.sourceSystem());
                canonical.setEventType(rec.eventType());
                canonical.setAmountPaise(rec.amountPaise());
                canonical.setCurrency("INR");
                canonical.setSourceStatus(rec.status());
                canonical.setCanonicalStatus(mapStatus(rec.status()));
                canonical.setReversalReference(rec.reversalReference());
                canonical.setSourceTimestamp(rec.timestamp() != null ? rec.timestamp() : OffsetDateTime.now());
                canonical.setReceivedTimestamp(OffsetDateTime.now());
                java.time.OffsetDateTime cutoff = parsedBatch.businessDate() != null
                        ? parsedBatch.businessDate().atTime(18, 0).atOffset(java.time.ZoneOffset.UTC)
                        : (rec.timestamp() != null ? rec.timestamp().toLocalDate().atTime(18, 0).atOffset(java.time.ZoneOffset.UTC) : OffsetDateTime.now());
                canonical.setCutoffTimestamp(cutoff);
                canonicalRepository.save(canonical);
                processed++;
            } else {
                quarantined++;
            }
        }

        BatchValidator.ValidationResult valResult = batchValidator.validate(parsedBatch);
        if (!valResult.isValid()) {
            batchEntity.setStatus(BatchStatus.QUARANTINED.name());
            batchRepository.save(batchEntity);
        } else {
            batchEntity.setStatus(BatchStatus.VALIDATED.name());
            batchRepository.save(batchEntity);
        }

        return new IngestionResult(total, processed, quarantined, skipped, batchEntity.getStatus());
    }

    private String mapStatus(String status) {
        if (status == null) return "PENDING";
        return switch (status.toUpperCase()) {
            case "SUCCESS", "COMPLETED", "DONE" -> "SUCCESS";
            case "FAILED", "ERROR" -> "FAILED";
            case "REVERSED", "REFUNDED" -> "REVERSED";
            default -> "PENDING";
        };
    }
}
