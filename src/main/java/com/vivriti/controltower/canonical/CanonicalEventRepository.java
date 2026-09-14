package com.vivriti.controltower.canonical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanonicalEventRepository extends JpaRepository<CanonicalEventEntity, String> {
    List<CanonicalEventEntity> findByLoanReference(String loanRef);
    List<CanonicalEventEntity> findByCorrelationId(String correlationId);
    List<CanonicalEventEntity> findByBusinessEventId(String businessEventId);
    List<CanonicalEventEntity> findBySourceSystem(String sourceSystem);
    List<CanonicalEventEntity> findBySourceSystemAndCanonicalStatus(String sourceSystem, String status);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CanonicalEventEntity c WHERE c.rawRecord.batch.batchId = :batchId")
    List<CanonicalEventEntity> findByBatchId(@org.springframework.data.repository.query.Param("batchId") String batchId);
}
