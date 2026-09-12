package com.vivriti.controltower.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivriti.controltower.canonical.AuditLogEntity;
import com.vivriti.controltower.canonical.AuditLogRepository;
import com.vivriti.controltower.common.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public AuditLogEntity log(String entityName, String entityId, String action, String actorId, Object beforeState, Object afterState, String reason, String ruleVersion) {
        AuditLogEntity audit = new AuditLogEntity();
        audit.setLogId(IdGenerator.newId());
        audit.setEntityName(entityName);
        audit.setEntityId(entityId);
        audit.setAction(action);
        audit.setActorId(actorId);

        audit.setBeforeState(toJsonString(beforeState));
        audit.setAfterState(toJsonString(afterState));

        audit.setReason(reason);
        audit.setRuleVersion(ruleVersion);
        audit.setCreatedAt(OffsetDateTime.now());

        return auditLogRepository.save(audit);
    }

    private String toJsonString(Object state) {
        if (state == null) return null;
        if (state instanceof String s) {
            String trimmed = s.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
                return trimmed;
            }
        }
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            return "{}";
        }
    }
}
