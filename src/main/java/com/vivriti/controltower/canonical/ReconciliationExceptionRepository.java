package com.vivriti.controltower.canonical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationExceptionEntity, String> {
    List<ReconciliationExceptionEntity> findByStatus(String status);
    List<ReconciliationExceptionEntity> findByBusinessEventId(String businessEventId);
    List<ReconciliationExceptionEntity> findByPartnerCode(String partnerCode);
    List<ReconciliationExceptionEntity> findByPriority(String priority);
}
