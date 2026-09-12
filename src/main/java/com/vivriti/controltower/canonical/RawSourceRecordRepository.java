package com.vivriti.controltower.canonical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawSourceRecordRepository extends JpaRepository<RawSourceRecordEntity, String> {

    @Query("SELECT r FROM RawSourceRecordEntity r WHERE r.batch.batchId = :batchId")
    List<RawSourceRecordEntity> findByBatchId(@Param("batchId") String batchId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RawSourceRecordEntity r WHERE r.batch.batchId = :batchId AND r.payloadHash = :payloadHash")
    boolean existsByBatchIdAndPayloadHash(@Param("batchId") String batchId, @Param("payloadHash") String payloadHash);

    @Query("SELECT COUNT(r) FROM RawSourceRecordEntity r WHERE r.batch.batchId = :batchId AND r.quarantined = true")
    long countByBatchIdAndQuarantinedTrue(@Param("batchId") String batchId);

    @Query("SELECT COUNT(r) FROM RawSourceRecordEntity r WHERE r.batch.batchId = :batchId AND r.quarantined = false")
    long countByBatchIdAndQuarantinedFalse(@Param("batchId") String batchId);
}
