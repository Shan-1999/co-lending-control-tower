package com.vivriti.controltower.audit;

import com.vivriti.controltower.canonical.AuditLogEntity;
import com.vivriti.controltower.canonical.AuditLogRepository;
import com.vivriti.controltower.canonical.CloseSummaryEntity;
import com.vivriti.controltower.canonical.CloseSummaryRepository;
import com.vivriti.controltower.canonical.SourceBatchEntity;
import com.vivriti.controltower.canonical.SourceBatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MakerCheckerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MakerCheckerGuard makerCheckerGuard;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CloseSummaryRepository closeSummaryRepository;

    @Autowired
    private SourceBatchRepository batchRepository;

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @Transactional
    void testOperatorCannotApprove() throws Exception {
        mockMvc.perform(post("/api/v1/close/approve").param("closeId", "DUMMY"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "approver", roles = {"APPROVER"})
    @Transactional
    void testApproverCanApprove() throws Exception {
        SourceBatchEntity batch = new SourceBatchEntity();
        batch.setBatchId("BATCH-SEC-1");
        batch.setPartnerCode("PARTNER_ALPHA");
        batch.setSourceSystem("ORIGINATOR");
        batch.setBusinessDate(LocalDate.now());
        batch.setDeclaredCount(1);
        batch.setDeclaredAmountPaise(100000L);
        batch.setStatus("VALIDATED");
        batchRepository.save(batch);

        CloseSummaryEntity summary = new CloseSummaryEntity();
        summary.setCloseId("CLOSE-SEC-1");
        summary.setBatch(batch);
        summary.setBusinessDate(LocalDate.now());
        summary.setDecision("HOLD");
        summary.setOpeningPositionPaise(0L);
        summary.setValidMovementsPaise(0L);
        summary.setReversalsPaise(0L);
        summary.setClosingPositionPaise(0L);
        summary.setMatchedPaise(0L);
        summary.setTimingPendingPaise(0L);
        summary.setUnresolvedExceptionPaise(0L);
        summary.setQuarantinedPaise(0L);
        summary.setUnaccountedPaise(0L);
        summary.setDecidedBy("user-1");
        summary.setDecidedAt(OffsetDateTime.now());
        closeSummaryRepository.save(summary);

        mockMvc.perform(post("/api/v1/close/approve").param("closeId", "CLOSE-SEC-1"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void testMakerCheckerSegregationLogic() {
        String batchId = "BATCH-SEC-2";

        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setLogId("LOG-1");
        auditLog.setEntityName("ReconciliationException");
        auditLog.setEntityId("EXC-1");
        auditLog.setAction("OVERRIDE");
        auditLog.setActorId("user_x");
        auditLog.setRuleVersion("v1.0");
        auditLog.setCreatedAt(OffsetDateTime.now());
        auditLogRepository.save(auditLog);

        assertThatThrownBy(() -> makerCheckerGuard.assertSegregation(batchId, "user_x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Maker cannot be Checker");

        assertThatCode(() -> makerCheckerGuard.assertSegregation(batchId, "user_y"))
                .doesNotThrowAnyException();
    }
}
