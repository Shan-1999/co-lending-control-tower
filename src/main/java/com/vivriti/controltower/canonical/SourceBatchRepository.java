package com.vivriti.controltower.canonical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SourceBatchRepository extends JpaRepository<SourceBatchEntity, String> {
    List<SourceBatchEntity> findByPartnerCodeAndBusinessDate(String partnerCode, LocalDate date);
    Optional<SourceBatchEntity> findByBatchId(String batchId);
}
