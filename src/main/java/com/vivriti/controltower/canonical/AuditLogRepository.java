package com.vivriti.controltower.canonical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
    List<AuditLogEntity> findByEntityNameAndEntityId(String entityName, String entityId);
    List<AuditLogEntity> findByActorId(String actorId);
    List<AuditLogEntity> findByActorIdAndEntityNameAndEntityId(String actorId, String entityName, String entityId);
}
