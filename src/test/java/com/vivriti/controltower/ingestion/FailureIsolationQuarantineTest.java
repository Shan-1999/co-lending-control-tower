package com.vivriti.controltower.ingestion;

import com.vivriti.controltower.canonical.CanonicalEventEntity;
import com.vivriti.controltower.canonical.CanonicalEventRepository;
import com.vivriti.controltower.canonical.RawSourceRecordEntity;
import com.vivriti.controltower.canonical.RawSourceRecordRepository;
import com.vivriti.controltower.canonical.SourceBatchEntity;
import com.vivriti.controltower.canonical.SourceBatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class FailureIsolationQuarantineTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private RawSourceRecordRepository rawRepository;

    @Autowired
    private CanonicalEventRepository canonicalRepository;

    @Autowired
    private SourceBatchRepository batchRepository;

    @Test
    @Transactional
    void testFailureIsolationAndQuarantine() {
        String batchId = "BATCH-ISO-TEST-1";
        
        ParsedFeedRecord validRecord = new ParsedFeedRecord(
                "LOAN-VALID", 10000L, "SUCCESS", OffsetDateTime.now(), "UTR-1",
                "LMS_BOOKING", "CORR-1", "EVT-1", "PARTNER_ALPHA", "LMS", null, "raw-payload-1"
        );

        ParsedFeedRecord missingLoanRefRecord = new ParsedFeedRecord(
                null, 10000L, "SUCCESS", OffsetDateTime.now(), "UTR-2",
                "LMS_BOOKING", "CORR-2", "EVT-2", "PARTNER_ALPHA", "LMS", null, "raw-payload-2"
        );

        ParsedFeedRecord negativeAmountRecord = new ParsedFeedRecord(
                "LOAN-NEG", -5000L, "SUCCESS", OffsetDateTime.now(), "UTR-3",
                "LMS_BOOKING", "CORR-3", "EVT-3", "PARTNER_ALPHA", "LMS", null, "raw-payload-3"
        );

        // Declared total is 50000, but sum is 10000 + 10000 - 5000 = 15000, causing drift
        ParsedBatch batch = new ParsedBatch(
                batchId, "PARTNER_ALPHA", "LMS", LocalDate.now(), 3, 50000L,
                List.of(validRecord, missingLoanRefRecord, negativeAmountRecord)
        );

        IngestionResult result = ingestionService.ingestBatch(batch);

        // valid -> processed = 1, missing/negative -> quarantined = 2
        assertThat(result.processedRecords()).isEqualTo(1);
        assertThat(result.quarantinedRecords()).isEqualTo(2);

        SourceBatchEntity savedBatch = batchRepository.findById(batchId).orElseThrow();
        // Since batch total drifts (50000 vs 15000), validator should fail the batch to QUARANTINED status
        assertThat(savedBatch.getStatus()).isEqualTo("QUARANTINED");

        List<RawSourceRecordEntity> rawRecords = rawRepository.findByBatchId(batchId);
        assertThat(rawRecords).hasSize(3);

        RawSourceRecordEntity rawMissingRef = rawRecords.stream()
                .filter(r -> "raw-payload-2".equals(r.getRawPayload()))
                .findFirst().orElseThrow();
        assertThat(rawMissingRef.isQuarantined()).isTrue();
        assertThat(rawMissingRef.getQuarantineReason()).contains("Missing loan reference");

        RawSourceRecordEntity rawNegAmt = rawRecords.stream()
                .filter(r -> "raw-payload-3".equals(r.getRawPayload()))
                .findFirst().orElseThrow();
        assertThat(rawNegAmt.isQuarantined()).isTrue();
        assertThat(rawNegAmt.getQuarantineReason()).contains("Negative amount without reversal flag");

        RawSourceRecordEntity rawValid = rawRecords.stream()
                .filter(r -> "raw-payload-1".equals(r.getRawPayload()))
                .findFirst().orElseThrow();
        assertThat(rawValid.isQuarantined()).isFalse();

        List<CanonicalEventEntity> canonicalEvents = canonicalRepository.findByLoanReference("LOAN-VALID");
        assertThat(canonicalEvents).hasSize(1);
        assertThat(canonicalEvents.get(0).getAmountPaise()).isEqualTo(10000L);
    }
}
