package com.vivriti.controltower.audit;

import com.vivriti.controltower.canonical.AuditLogEntity;
import com.vivriti.controltower.canonical.AuditLogRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MakerCheckerGuard {

    private final AuditLogRepository auditLogRepository;

    public MakerCheckerGuard(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void assertSegregation(String batchId, String approverId) {
        List<AuditLogEntity> overrides = auditLogRepository.findByActorId(approverId).stream()
                .filter(a -> "ReconciliationException".equals(a.getEntityName()) && "OVERRIDE".equals(a.getAction()))
                .toList();

        // In a real system, we map exceptions back to the batchId. For simplicity, if they overrode any exception we block.
        // Assuming we would map batchId properly:
        if (!overrides.isEmpty()) {
            throw new RuntimeException("Segregation of duties violation: Maker cannot be Checker");
        }
    }
}
