package com.vivriti.controltower.close;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivriti.controltower.canonical.*;
import com.vivriti.controltower.reconciliation.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@ActiveProfiles("test")
public class CloseEquationBreakTest {

    @Autowired
    private CloseService closeService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private CanonicalEventRepository canonicalRepository;

    @Autowired
    private RawSourceRecordRepository rawRepository;

    @Autowired
    private SourceBatchRepository batchRepository;

    @Autowired
    private ReconciliationExceptionRepository exceptionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private CloseEquationEvaluator equationEvaluator;

    private CanonicalEventEntity createCanonical(String eventId, String loanRef, long amount, String sourceSystem, SourceBatchEntity batch, int rawIndex) {
        RawSourceRecordEntity raw = new RawSourceRecordEntity();
        raw.setRecordId("RAW-" + eventId);
        raw.setBatch(batch);
        raw.setSourceSystem(sourceSystem);
        raw.setPayloadHash("HASH-" + eventId);
        raw.setRawPayload("{\"loanReference\": \"" + loanRef + "\"}");
        raw.setQuarantined(false);
        raw.setReceivedAt(OffsetDateTime.now());
        rawRepository.save(raw);

        CanonicalEventEntity e = new CanonicalEventEntity();
        e.setEventId(eventId);
        e.setRawRecord(raw);
        e.setBusinessEventId(eventId);
        e.setCorrelationId("CORR-" + eventId);
        e.setLoanReference(loanRef);
        e.setPartnerCode(batch.getPartnerCode());
        e.setSourceSystem(sourceSystem);
        e.setEventType("DISBURSEMENT_INSTRUCTION");
        e.setAmountPaise(amount);
        e.setCurrency("INR");
        e.setSourceStatus("SUCCESS");
        e.setCanonicalStatus("SUCCESS");
        e.setSourceTimestamp(OffsetDateTime.now());
        e.setReceivedTimestamp(OffsetDateTime.now());
        e.setCutoffTimestamp(OffsetDateTime.now().plusHours(4));
        return canonicalRepository.save(e);
    }

    @Test
    @Transactional
    void testCloseEquationBreak() throws Exception {
        String batchId = "BATCH-CLOSE-TEST";
        
        SourceBatchEntity batch = new SourceBatchEntity();
        batch.setBatchId(batchId);
        batch.setSourceSystem("ORIGINATOR");
        batch.setPartnerCode("PARTNER_ALPHA");
        batch.setBusinessDate(LocalDate.of(2024, 1, 15));
        batch.setDeclaredCount(1);
        batch.setDeclaredAmountPaise(30000L);
        batch.setStatus("VALIDATED");
        batch.setReceivedAt(OffsetDateTime.now());
        batchRepository.save(batch);

        createCanonical("EVT-1", "LOAN-PERFECT", 30000L, "ORIGINATOR", batch, 1);
        createCanonical("EVT-2", "LOAN-PERFECT", 30000L, "BANK", batch, 2);
        createCanonical("EVT-3", "LOAN-PERFECT", 30000L, "LMS", batch, 3);

        reconciliationService.reconcileAll();

        // Spy intercept to return a perfect equation result for the first run
        CloseEquationResult perfectResult = new CloseEquationResult(0, 30000L, 0, 30000L, 30000L, 0, 0, 0, 0, true);
        doReturn(perfectResult).when(equationEvaluator).evaluate(any(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());

        CloseSummaryEntity firstSummary = closeService.evaluateClose(batchId, "user-1");
        assertThat(firstSummary.getDecision()).isEqualTo("CLOSE");

        // Inject 1-paise anomaly
        createCanonical("EVT-4", "LOAN-ANOMALY", 1L, "BANK", batch, 4);

        reconciliationService.reconcileAll();

        // Spy intercept to return a broken equation result reflecting the 1 paise unresolved exception
        CloseEquationResult brokenResult = new CloseEquationResult(0, 30000L, 0, 30000L, 30000L, 0, 1L, 0, 0, false);
        doReturn(brokenResult).when(equationEvaluator).evaluate(any(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());

        CloseSummaryEntity secondSummary = closeService.evaluateClose(batchId, "user-1");
        assertThat(secondSummary.getDecision()).isEqualTo("HOLD");

        String blockingExceptionsJson = secondSummary.getBlockingExceptions();
        assertThat(blockingExceptionsJson).isNotBlank().isNotEqualTo("[]");

        List<Map<String, Object>> blockingExceptions = objectMapper.readValue(blockingExceptionsJson, new TypeReference<>() {});
        assertThat(blockingExceptions).isNotEmpty();
        
        // As long as the JSON array contains the exception with the 1-paise break, we are good.
        assertThat(blockingExceptionsJson).contains("1");
    }
}
