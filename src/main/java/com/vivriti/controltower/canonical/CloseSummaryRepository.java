package com.vivriti.controltower.canonical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CloseSummaryRepository extends JpaRepository<CloseSummaryEntity, String> {

    @Query("SELECT c FROM CloseSummaryEntity c WHERE c.batch.batchId = :batchId")
    Optional<CloseSummaryEntity> findByBatchId(@Param("batchId") String batchId);

    List<CloseSummaryEntity> findByBusinessDate(LocalDate date);
}
